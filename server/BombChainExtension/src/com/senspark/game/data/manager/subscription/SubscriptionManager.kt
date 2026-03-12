package com.senspark.game.data.manager.subscription

import com.senspark.game.data.model.config.SubscriptionPackage
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.customEnum.SubscriptionProduct
import com.senspark.game.exception.CustomException

class SubscriptionManager(
    private val _shopDataAccess: IShopDataAccess,
) : ISubscriptionManager {
    private val _configs: MutableMap<SubscriptionProduct, SubscriptionPackage> = mutableMapOf()
    override val configList: MutableList<SubscriptionPackage> = mutableListOf()

    override fun initialize() {
        _configs.putAll(_shopDataAccess.loadSubscription())
        configList.addAll(_configs.values.toList().sortedBy { it.no })
    }

    override fun getSubscription(subscriptionProduct: SubscriptionProduct): SubscriptionPackage {
        return _configs[subscriptionProduct]
            ?: throw CustomException("Subscription id ${subscriptionProduct.name} invalid")
    }
}