package com.senspark.game.data.manager.iap

import com.senspark.common.service.IServerService
import com.senspark.game.api.IVerifyIapBillResult
import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.config.IAPShopConfig
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.customEnum.IAPShopType
import com.senspark.game.declare.customTypeAlias.ProductId

interface IIAPShopManager : IServerService {
    fun getShopConfigs(type: IAPShopType): List<IAPShopConfig>
    fun getGoldShopConfigs(): List<IAPGoldShopConfigItem>
    fun buyGold(userController: IUserController, itemId: Int)
    suspend fun getFreeRewardByAds(userController: IUserController, token: String, rewardType: EnumConstants.BLOCK_REWARD_TYPE)
    fun getUserFreeRewardConfigs(userController: IUserController): List<UserFreeRewardConfigResponseItem>
    fun getShopConfigs(type: IAPShopType, productId: ProductId): IAPShopConfig
    fun verifyBillInfo(
        packageName: String,
        productId: String,
        billToken: String,
        transactionId: String,
        store: String
    ): IVerifyIapBillResult
}