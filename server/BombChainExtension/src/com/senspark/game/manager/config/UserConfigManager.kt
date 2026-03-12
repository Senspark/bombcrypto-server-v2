package com.senspark.game.manager.config

import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.manager.gacha.IGachaChestSlotManager
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IRewardDataAccess
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.db.model.UserConfig
import com.senspark.game.db.model.UserFreeRewardConfig
import com.senspark.game.db.model.UserGachaChestSlot
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.blockReward.IUserBlockRewardManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

class UserConfigManager(
    private val _mediator: UserControllerMediator,
    private val _userBlockRewardManager: IUserBlockRewardManager,
    private val _noAds: () -> Boolean,
) : IUserConfigManager {
    private val _dataAccessManager = _mediator.services.get<IDataAccessManager>()
    private val _shopDataAccess: IShopDataAccess = _dataAccessManager.shopDataAccess
    private val _userDataAccess: IUserDataAccess = _dataAccessManager.userDataAccess
    private val _rewardDataAccess: IRewardDataAccess = _dataAccessManager.rewardDataAccess
    private val _gachaChestSlotManager = _mediator.svServices.get<IGachaChestSlotManager>()
        
    private lateinit var _config: UserConfig

    private fun getConfig(): UserConfig {
        if (!::_config.isInitialized) {
            loadUserConfig()
        }
        return _config
    }

    private fun loadUserConfig() {
        _config = _userDataAccess.loadUserConfig(_mediator.userId, _gachaChestSlotManager)
    }
    
    override val userGachaChestSlots get() = getConfig().userGachaChestSlots
    override val numberChestSlot get() = getConfig().numberChestSlot
    override val freeRewardConfig: UserFreeRewardConfig get() = getConfig().userFreeRewardConfig
    override val miscConfigs: MiscConfigs get() = getConfig().miscConfigs
    override var cachedPurchasedPacks: MutableList<String>? = null
    override val lastTimeClaimSubscription: Instant?
        get() = getConfig().lastTImeClaimSubscription

    override val noAds: Boolean
        get() {
            return getConfig().noAds || _noAds()
        }
    override val isReceivedFirstChestSkipTime get() = getConfig().isReceivedFirstChestSkipTime
    override val isReceivedTutorialReward get() = getConfig().isReceivedTutorialReward
    override val totalCostumePresetSlot get() = getConfig().totalCostumePresetSlot
    
    override fun reloadConfig() {
        loadUserConfig()
    }

    override fun buyGachaChestSlot(slot: Int) {
        val slotConfig = _gachaChestSlotManager.slots[slot] ?: throw CustomException("Slot not exists")
        val gem = _userBlockRewardManager.get(BLOCK_REWARD_TYPE.GEM)?.values ?: 0F
        val gemLocked = _userBlockRewardManager.get(BLOCK_REWARD_TYPE.GEM_LOCKED)?.values ?: 0F
        if (slotConfig.price > (gem + gemLocked))
            throw CustomException("Not enough ${slotConfig.price} Gem")

        val userSlots = getConfig().userGachaChestSlots.map {
            UserGachaChestSlot(
                it.slotNumber,
                it.slotType,
                it.isOwner,
                it.chest,
                it.price
            )
        }.toMutableList()
        val userSlot = userSlots.firstOrNull { it.slotNumber == slot }
        if (userSlot == null) {
            userSlots.add(UserGachaChestSlot(slotConfig.slot, slotConfig.type, true))
        } else if (!userSlot.isOwner) {
            userSlot.isOwner = true
        } else {
            throw CustomException("You already bough this slot")
        }

        _userDataAccess.updateUserGachaChestSlot(
            _mediator.userId,
            EnumConstants.DataType.TR,
            slotConfig.price,
            slot,
            Json.encodeToString(userSlots)
        )
        loadUserConfig()
    }

    override fun buyCostumePresetSlot(rewardType: BLOCK_REWARD_TYPE) {
        val price = _shopDataAccess.loadCostumePresetPrice()[rewardType]
            ?: throw CustomException("Reward type not exists")
        _userBlockRewardManager.checkEnoughReward(price.toFloat(), rewardType)
        _rewardDataAccess.addTRRewardForUser(
            _mediator.userId,
            _mediator.dataType,
            emptyList(),
            {},
            "Buy_custom_preset_slot",
            mapOf(rewardType to price.toFloat()),
            emptyList()
        )
        getConfig().totalCostumePresetSlot += 1
        _userDataAccess.updateUserConfig(_mediator.userId, getConfig())
        loadUserConfig()
    }

    override fun setNoAds() {
        _userDataAccess.saveUserConfigNoAds(_mediator.userId, true)
        loadUserConfig()
    }

    override fun setReceivedFirstChestSkipTime() {
        getConfig().isReceivedFirstChestSkipTime = true
        _userDataAccess.updateUserConfig(_mediator.userId, getConfig())
        loadUserConfig()
    }

    override fun changeUserFreeRewardOpenTimeConfigToNow(rewardType: BLOCK_REWARD_TYPE) {
        if (rewardType == BLOCK_REWARD_TYPE.GEM_LOCKED)
            freeRewardConfig.lastTimeGetFreeGems = Instant.now().toEpochMilli()
        else if (rewardType == BLOCK_REWARD_TYPE.GOLD)
            freeRewardConfig.lastTimeGetFreeGolds = Instant.now().toEpochMilli()
    }

    override fun saveMiscConfigs() {
        _userDataAccess.saveUserMiscConfig(_mediator.userId, miscConfigs)
    }
}