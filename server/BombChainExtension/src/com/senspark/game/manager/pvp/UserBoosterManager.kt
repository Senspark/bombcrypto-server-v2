package com.senspark.game.manager.pvp

import com.senspark.game.constant.Booster
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.model.user.UserBooster
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.manager.user.IUserOldItemManager
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray

class UserBoosterManager(
    private val _mediator: UserControllerMediator,
    private val _userOldItemManger: IUserOldItemManager,
) : IUserBoosterManager {

    private val _dataAccessManager = _mediator.services.get<IDataAccessManager>()

    /**
     * booster cong thang chi so khi bat dau game
     */
    private val initBoosters = setOf(
        Booster.RangePlusOne.value,
        Booster.BombPlusOne.value,
        Booster.SpeedPlusOne.value
    )

    /**
     * WARNING: should not use directly, use getBooster() instead
     */
    private lateinit var _boosters: Map<Int, UserBooster>

    private fun getBoosters(): Map<Int, UserBooster> {
        if (!::_boosters.isInitialized) {
            loadFromDb()
        }
        return _boosters
    }

    override fun loadFromDb() {
        _boosters = _dataAccessManager.userDataAccess.loadUserPvpBoosters(_mediator.userId)
    }

    override fun getBooster(itemId: Int): UserBooster? {
        return getBoosters()[itemId]
    }

    @Throws(Exception::class)
    override fun chooseBooster(itemId: Int, chosen: Boolean) {
        val booster = getBooster(itemId) ?: throw Exception("Could not find booster: $itemId")
        if (chosen) {
            if (!booster.hasQuantity(1)) throw Exception("Not enough booster: $itemId")
        }
        booster.select(chosen)
        if (initBoosters.contains(booster.itemId)) {
            val used = booster.applyBooster()
            if (used) {
                _mediator.setUsedPvpBoosterToDatabase(booster.itemId)
            }
        }
    }

    override fun usePvpBooster(boosterValues: List<Int>, isWhiteList: Boolean) {
        boosterValues.forEach {
            val booster = getBooster(it) ?: throw Exception("Could not find booster: $it")
            booster.applyBooster(isWhiteList)
        }
    }

    override fun toSfsArray(): ISFSArray {
        val sfsArray = SFSArray()
        getBoosters().values.forEach {
            sfsArray.addSFSObject(it.toSfsObject())
        }
        return sfsArray
    }

    override fun getSelectBoosters(): List<UserBooster> {
        return getBoosters().filter { it.value.selected }.toList().map { it.second }
    }

    override fun saveUsedBooster() {
        val boosterUsed = getBoosters().filter { it.value.used }
        // mark old item
        boosterUsed.forEach {
            _userOldItemManger.checkAndAddOldItem(it.value.itemId)
        }
        _dataAccessManager.userDataAccess.subUserPvpBoosters(
            _mediator.userId, boosterUsed.values.flatMap { it.listUsed }
        )
        this.loadFromDb()
    }
}