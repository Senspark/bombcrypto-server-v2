package com.senspark.game.manager.rental

import com.senspark.common.utils.ILogger
import com.senspark.game.constant.StreamKeys
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.SFSField
import com.senspark.game.manager.IUsersManager
import com.senspark.game.pvp.HandlerCommand
import com.senspark.game.utils.JsonExtensionBuilder
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.Serializable

/**
 * Applies P2P house-rental events published by the marketplace backend.
 *
 * The database is always the source of truth (the site already wrote to it);
 * this manager only refreshes the in-memory state of players who happen to be
 * online and pushes the update so they see it without relogging.
 */
class HouseRentalResponseManager(
    private val _usersManager: IUsersManager,
    private val _logger: ILogger,
) : IHouseRentalResponseManager {

    private val tag = "[${StreamKeys.AP_RENTAL_SYNC}]"

    private companion object {
        const val EVENT_RENTED = "RENTED"
    }

    override fun listenRentalSync(message: String): Boolean {
        try {
            _logger.log("$tag Received rental event: $message")
            val event = JsonExtensionBuilder.json.decodeFromString<RentalEvent>(message)
            val dataType = EnumConstants.DataType.valueOf(event.type)

            // The renter gains or loses the house and had money moved
            event.renter_uid?.let { renterUid ->
                getController(renterUid, dataType)?.let { controller ->
                    controller.masterUserManager.houseManager.refreshRentedHouses()
                    controller.masterUserManager.blockRewardManager.loadUserBlockReward()
                    push(controller, event)
                }
            }

            // The owner gets paid, and gets the house back when the rental ends
            getController(event.owner_uid, dataType)?.let { controller ->
                if (event.event == EVENT_RENTED) {
                    releaseHouseFromOwner(controller, event.house_id)
                }
                controller.masterUserManager.blockRewardManager.loadUserBlockReward()
                push(controller, event)
            }

            return true
        } catch (e: Exception) {
            _logger.error("$tag Error when processing: $e")
            return false
        }
    }

    private fun getController(uid: Int, dataType: EnumConstants.DataType): IUserController? {
        return _usersManager.getUserController(uid, dataType, EnumConstants.Landing.TREASURE)
    }

    /**
     * The house was just rented out: its owner cannot use it anymore. The active
     * flag is already cleared in the database by sp_rent_house_p2p, so here we
     * only fix the in-memory state and put the heroes inside back to sleep.
     */
    private fun releaseHouseFromOwner(controller: IUserController, houseId: Int) {
        val houseManager = controller.masterUserManager.houseManager
        val house = houseManager.getHouse(houseId) ?: return
        if (!house.isActive) {
            return
        }

        house.isActive = false

        val heroManager = controller.masterUserManager.heroFiManager
        val heroesInside = heroManager.housingHeroes
        if (heroesInside.isEmpty()) {
            return
        }

        heroesInside.forEach { heroManager.setSleep(it) }
        _logger.log("$tag Owner ${controller.userId} lost house $houseId to a rental, heroes sent to sleep")
    }

    private fun push(controller: IUserController, event: RentalEvent) {
        val result: ISFSObject = SFSObject()
        result.putUtfString("event", event.event)
        result.putInt("house_id", event.house_id)
        event.day_number?.let { result.putInt("day_number", it) }
        event.amount?.let { result.putDouble("amount", it) }
        event.pay_token?.let { result.putUtfString("pay_token", it) }
        // Same balance payload the client already parses on deposit sync
        result.putSFSArray(SFSField.Rewards, controller.masterUserManager.blockRewardManager.toSfsArrays())

        controller.sendDataEncryption(HandlerCommand.HouseRentalUpdateResponse, result, true)
    }

    override fun initialize() {
    }

    @Serializable
    data class RentalEvent(
        val event: String,
        val house_id: Int,
        val type: String,
        val owner_uid: Int,
        val renter_uid: Int? = null,
        val rental_id: String? = null,
        val listing_id: String? = null,
        val day_number: Int? = null,
        val amount: Double? = null,
        val pay_token: String? = null,
    )
}
