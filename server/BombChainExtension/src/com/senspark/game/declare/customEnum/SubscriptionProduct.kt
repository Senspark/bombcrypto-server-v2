package com.senspark.game.declare.customEnum

import com.senspark.game.exception.CustomException

enum class SubscriptionProduct(
    val level: Int,
    val normalizeName: String
) {
    STANDARD(1, "subscription_standard"),
    DELUXE(2, "subscription_deluxe"),
    PREMIUM(3, "subscription_premium");


    companion object {
        val items = SubscriptionProduct.values().associateBy { it.normalizeName }
        fun fromNormalizeName(normalizeName: String): SubscriptionProduct {
            return items[normalizeName] ?: throw CustomException("Product with name $normalizeName not exists")
        }
    }
}