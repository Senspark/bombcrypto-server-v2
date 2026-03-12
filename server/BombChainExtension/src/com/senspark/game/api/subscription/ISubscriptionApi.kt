package com.senspark.game.api.subscription

import com.senspark.game.declare.customEnum.IapStore
import com.senspark.game.declare.customEnum.SubscriptionProduct

interface ISubscriptionApi {
    fun verifySubscription(
        subscriptionProduct: SubscriptionProduct,
        userToken: String,
        redirect: IapStore,
    ): VerifySubscription

    fun cancelSubscription(
        subscriptionProduct: SubscriptionProduct,
        userToken: String,
        redirect: IapStore,
    )
}