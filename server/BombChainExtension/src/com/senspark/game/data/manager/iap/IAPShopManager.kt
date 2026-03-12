package com.senspark.game.data.manager.iap

import com.senspark.common.utils.ILogger
import com.senspark.game.api.*
import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.config.IAPShopConfig
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.db.model.UserFreeRewardConfig
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.customEnum.ChangeRewardReason
import com.senspark.game.declare.customEnum.IAPShopType
import com.senspark.game.declare.customTypeAlias.ProductId
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.IEnvManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.sql.ResultSet
import java.time.Instant

class IAPShopManager(
    private val _shopDataAccess: IShopDataAccess,
    envManager: IEnvManager,
    private val _verifyAdApi: IVerifyAdApiManager,
    api: IRestApi,
    logger: ILogger,
) : IIAPShopManager {
    private val _verifyApi: IVerifyIAPBillApi = VerifyIAPBillApi(envManager, api, logger)
    private val _iapShopConfigs: MutableMap<IAPShopType, Map<ProductId, IAPShopConfig>> = mutableMapOf()
    private val _goldShopConfigs: MutableList<IAPGoldShopConfigItem> = mutableListOf()
    private val _freeRewardConfigs: MutableList<FreeRewardConfigItem> = mutableListOf()
    private lateinit var _freeGemRewardConfig: FreeRewardConfigItem
    private lateinit var _freeGoldRewardConfig: FreeRewardConfigItem

    override fun initialize() {
        _iapShopConfigs.putAll(_shopDataAccess.getIAPGemShopConfigs())
        _goldShopConfigs.addAll(_shopDataAccess.getIAPGoldShopConfigs())
        _freeRewardConfigs.addAll(_shopDataAccess.getFreeRewardConfigs())
        _freeGemRewardConfig = _freeRewardConfigs.first { it.rewardType == BLOCK_REWARD_TYPE.GEM_LOCKED.toString() }
        _freeGoldRewardConfig = _freeRewardConfigs.first { it.rewardType == BLOCK_REWARD_TYPE.GOLD.toString() }
    }

    override fun getShopConfigs(type: IAPShopType): List<IAPShopConfig> {
        return _iapShopConfigs[type]?.values?.toList()?.sortedBy {
            it.items.firstOrNull()?.quantity ?: 0
        } ?: emptyList()
    }

    override fun getShopConfigs(type: IAPShopType, productId: ProductId): IAPShopConfig {
        return _iapShopConfigs[type]?.get(productId) ?: throw CustomException("getGemShopConfig must be not null")
    }

    override fun getGoldShopConfigs(): List<IAPGoldShopConfigItem> = _goldShopConfigs

    override fun buyGold(userController: IUserController, itemId: Int) {
        val itemConfig = _goldShopConfigs.firstOrNull { it.itemId == itemId }
            ?: throw Exception("Wrong item id: $itemId")
        _shopDataAccess.buyGoldByGem(
            userController.userInfo,
            userController.masterUserManager.blockRewardManager,
            itemConfig
        )
    }

    override fun verifyBillInfo(
        packageName: String,
        productId: String,
        billToken: String,
        transactionId: String,
        store: String
    ): IVerifyIapBillResult {
        return _verifyApi.verify(packageName, productId, billToken, transactionId, store)
    }

    override suspend fun getFreeRewardByAds(userController: IUserController, token: String, rewardType: BLOCK_REWARD_TYPE) {
        val lastTime: Long
        val config: FreeRewardConfigItem
        when (rewardType) {
            BLOCK_REWARD_TYPE.GEM_LOCKED -> {
                lastTime = userController.masterUserManager.userConfigManager.freeRewardConfig.lastTimeGetFreeGems
                config = _freeGemRewardConfig
            }

            BLOCK_REWARD_TYPE.GOLD -> {
                lastTime = userController.masterUserManager.userConfigManager.freeRewardConfig.lastTimeGetFreeGolds
                config = _freeGoldRewardConfig
            }

            else -> throw Exception("Not found config: $rewardType")
        }
        if (lastTime > 0) {
            val nextTime = lastTime + config.intervalInMinutes * 60 * 1000
            if (nextTime > Instant.now().toEpochMilli())
                throw Exception("Wait for next time, please!")
        }
        
        val isValid = _verifyAdApi.isValidAds(token)
 
        if (!isValid)
            throw Exception("Invalid token: $token")
        _shopDataAccess.addFreeReward(userController, config, ChangeRewardReason.FREE_ADS_REWARD)
    }

    override fun getUserFreeRewardConfigs(userController: IUserController): List<UserFreeRewardConfigResponseItem> {
        val userConfig = userController.masterUserManager.userConfigManager.freeRewardConfig
        return listOf(
            getUserFreeConfigResponseByRewardType(userConfig, BLOCK_REWARD_TYPE.GEM_LOCKED),
            getUserFreeConfigResponseByRewardType(userConfig, BLOCK_REWARD_TYPE.GOLD)
        )
    }

    private fun getUserFreeConfigResponseByRewardType(
        userFreeRewardConfig: UserFreeRewardConfig?,
        rewardType: BLOCK_REWARD_TYPE
    ): UserFreeRewardConfigResponseItem {
        val config = _freeRewardConfigs.firstOrNull { it.rewardType == rewardType.toString() }
            ?: throw Exception("Not found config for reward type: $rewardType")
        var nextTime = 0L
        userFreeRewardConfig?.apply {
            nextTime = if (rewardType == BLOCK_REWARD_TYPE.GEM_LOCKED)
                userFreeRewardConfig.lastTimeGetFreeGems + (config.intervalInMinutes * 60 * 1000)
            else
                userFreeRewardConfig.lastTimeGetFreeGolds + (config.intervalInMinutes * 60 * 1000)
            if (nextTime <= Instant.now().toEpochMilli())
                nextTime = 0L
        }
        return UserFreeRewardConfigResponseItem(config.rewardType, config.quantityPerView, nextTime)
    }
}

@Serializable
data class UserFreeRewardConfigResponseItem(
    @SerialName("reward_type")
    val rewardType: String,
    @SerialName("quantity_per_view")
    val quantityPerView: Int,
    @SerialName("next_time")
    val nextTime: Long
)

class FreeRewardConfigItem(val rewardType: String, val quantityPerView: Int, val intervalInMinutes: Int) {
    companion object {
        @JvmStatic
        fun fromResultSet(rs: ResultSet): FreeRewardConfigItem {
            return FreeRewardConfigItem(
                rs.getString("reward_type"),
                rs.getInt("quantity_per_view"),
                rs.getInt("interval_in_minutes")
            )
        }
    }
}

class IAPGoldShopConfigItem(
    val itemId: Int,
    val name: String,
    val gemPrice: Int,
    val goldsReceive: Int
) {
    companion object {
        @JvmStatic
        fun fromResultSet(rs: ResultSet): IAPGoldShopConfigItem {
            return IAPGoldShopConfigItem(
                rs.getInt("item_id"),
                rs.getString("name"),
                rs.getInt("gem_price"),
                rs.getInt("golds_receive")
            )
        }
    }

    fun toSFSObject(): ISFSObject {
        return SFSObject.newInstance().apply {
            putInt("item_id", itemId)
            putUtfString("name", name)
            putInt("gem_price", gemPrice)
            putInt("golds_receive", goldsReceive)
        }
    }
}