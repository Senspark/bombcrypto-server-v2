package com.senspark.game.manager.subscription

import com.senspark.game.data.model.config.SubscriptionPackage
import com.senspark.game.data.model.user.UserSubscription
import com.senspark.game.declare.customEnum.IapStore
import com.senspark.game.declare.customEnum.SubscriptionProduct
import com.senspark.game.manager.config.IUserConfigManager
import com.smartfoxserver.v2.entities.data.ISFSArray

interface IUserSubscriptionManager {
    val subscriptionPackage: SubscriptionPackage?
    val noAds: Boolean
    val offlineRewardBonus: Int
    val pvpPenalty: Boolean
    val bonusPvpPoint: Float
    val adventureBonusItems: Float
    val gemPackageBonus: Float
    val subscriptionPackages: ISFSArray
    fun getValidSubscription(): UserSubscription?
    fun takeSubscriptionRewards()
    fun subscribe(subscriptionProduct: SubscriptionProduct, userToken: String, store: IapStore)
    fun cancelSubscribe(subscriptionProduct: SubscriptionProduct)
    fun initUserConfigManager(userConfigManager: IUserConfigManager)
} 