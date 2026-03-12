package com.senspark.game.db.model

import com.senspark.game.data.manager.gacha.IGachaChestSlotManager
import com.senspark.game.data.model.user.UserGachaChest
import com.senspark.game.db.model.GachaChestSlotType.FREE
import com.senspark.game.manager.config.MiscConfigs
import com.senspark.game.utils.deserialize
import com.senspark.game.utils.deserializeList
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant


enum class GachaChestSlotType {
    FREE,
    VIP,
    BUY
}

@Serializable
data class UserGachaChestSlot(
    val slotNumber: Int,
    @Transient
    var slotType: GachaChestSlotType = FREE,
    var isOwner: Boolean,
    @Transient
    var chest: UserGachaChest? = null,
    @Transient
    var price: Int = 0
)

@Serializable
data class UserFreeRewardConfig(
    var lastTimeGetFreeGems: Long,
    var lastTimeGetFreeGolds: Long
)

class UserConfig(
    override val uid: Int,
    private val _gachaChestSlotManager: IGachaChestSlotManager,
    var userFreeRewardConfig: UserFreeRewardConfig,
    gachaChestSlots: String? = null,
    miscConfigsJson: String? = null,
    val lastTImeClaimSubscription: Instant? = null,
    val noAds: Boolean = false,
    var isReceivedFirstChestSkipTime: Boolean = false,
    var isReceivedTutorialReward: Boolean = false,
    var totalCostumePresetSlot: Int = 0,
) : IUserConfig {


    val miscConfigs: MiscConfigs = if (miscConfigsJson.isNullOrEmpty()) {
        MiscConfigs()
    } else {
        deserialize(miscConfigsJson)
    }

    val numberChestSlot: Int
    override val userGachaChestSlots: List<UserGachaChestSlot> = if (gachaChestSlots.isNullOrEmpty()) {
        _gachaChestSlotManager.slots.map { (_, v) ->
            UserGachaChestSlot(
                v.slot,
                v.type,
                v.type == FREE,
                price = v.price
            )
        }
    } else {
        deserializeList<UserGachaChestSlot>(gachaChestSlots).map {
            val config =
                _gachaChestSlotManager.slots[it.slotNumber] ?: throw Exception("Cannot find slot ${it.slotNumber}")
            it.apply {
                price = config.price
                slotType = config.type
            }
        }
    }.sortedBy { it.slotNumber }

    init {
        numberChestSlot = userGachaChestSlots.filter { it.isOwner }.size
    }
}