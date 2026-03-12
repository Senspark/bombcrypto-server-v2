package com.senspark.game.manager.subscription

import com.senspark.game.data.model.config.SubscriptionPackage
import com.senspark.game.data.model.user.UserSubscription
import com.senspark.game.declare.customEnum.IapStore
import com.senspark.game.declare.customEnum.SubscriptionProduct
import com.senspark.game.manager.config.IUserConfigManager
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray

class NullUserSubscriptionManager : IUserSubscriptionManager {

    override fun getValidSubscription(): UserSubscription? {
        return null
    }

    override fun initUserConfigManager(userConfigManager: IUserConfigManager) {
        // Ton User chưa có tính năng này
    }

    override val subscriptionPackages: ISFSArray
        get() = SFSArray()

    override val subscriptionPackage: SubscriptionPackage?
        get() = null

    override val noAds: Boolean
        get() = false

    override val offlineRewardBonus: Int
        get() = 0

    override val pvpPenalty: Boolean
        get() = true

    override val bonusPvpPoint: Float
        get() = 0f

    override val adventureBonusItems: Float
        get() = 0f

    override val gemPackageBonus: Float
        get() = 0f

    override fun subscribe(subscriptionProduct: SubscriptionProduct, userToken: String, store: IapStore) {
        // Ton User chưa có tính năng này
    }

    override fun cancelSubscribe(subscriptionProduct: SubscriptionProduct) {
        // Ton User chưa có tính năng này
    }

    override fun takeSubscriptionRewards() {
        // Ton User chưa có tính năng này
    }
}