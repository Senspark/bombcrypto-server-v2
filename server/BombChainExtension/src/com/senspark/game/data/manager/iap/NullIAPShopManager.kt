package com.senspark.game.data.manager.iap

import com.senspark.game.api.*
import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.config.IAPShopConfig
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.customEnum.IAPShopType
import com.senspark.game.declare.customTypeAlias.ProductId
import com.senspark.game.exception.CustomException

class NullIAPShopManager : IIAPShopManager {

    override fun initialize() {
    }

    override fun getShopConfigs(type: IAPShopType): List<IAPShopConfig> {
        return emptyList()
    }

    override fun getShopConfigs(type: IAPShopType, productId: ProductId): IAPShopConfig {
        throw CustomException("Feature not support")
    }

    override fun getGoldShopConfigs(): List<IAPGoldShopConfigItem> = emptyList()

    override fun buyGold(userController: IUserController, itemId: Int) {}

    override fun verifyBillInfo(
        packageName: String,
        productId: String,
        billToken: String,
        transactionId: String,
        store: String
    ): IVerifyIapBillResult {
        throw CustomException("Feature not support")
    }

    override suspend fun getFreeRewardByAds(userController: IUserController, token: String, rewardType: BLOCK_REWARD_TYPE) {}

    override fun getUserFreeRewardConfigs(userController: IUserController): List<UserFreeRewardConfigResponseItem> {
        return emptyList()
    }
}