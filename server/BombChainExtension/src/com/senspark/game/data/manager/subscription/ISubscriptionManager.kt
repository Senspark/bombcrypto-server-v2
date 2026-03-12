package com.senspark.game.data.manager.subscription

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.config.SubscriptionPackage
import com.senspark.game.declare.customEnum.SubscriptionProduct

interface ISubscriptionManager : IServerService {
    fun getSubscription(subscriptionProduct: SubscriptionProduct): SubscriptionPackage
    val configList: List<SubscriptionPackage>
} 