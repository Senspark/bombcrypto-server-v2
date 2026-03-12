package com.senspark.game.data.manager.subscription

import com.senspark.game.data.model.config.SubscriptionPackage
import com.senspark.game.declare.customEnum.SubscriptionProduct
import com.senspark.game.exception.CustomException

class NullSubscriptionManager : ISubscriptionManager {

    override val configList get() = emptyList<SubscriptionPackage>()

    override fun initialize() {
    }

    override fun getSubscription(subscriptionProduct: SubscriptionProduct): SubscriptionPackage {
        throw CustomException("Feature not support")
    }
}