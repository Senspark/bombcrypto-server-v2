package com.senspark.game.api

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.config.MarketItemConfig
import com.senspark.game.data.model.config.MarketplaceItemGrouped
import com.senspark.game.data.model.config.MyItemMarket
import com.senspark.game.manager.offlineReward.Quantity
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

interface IMarketApi : IServerService {
    fun order(orderData: OrderDataRequest): OrderDataResponse
    fun cancelOrder(buyerUid: Int): Boolean
    fun buy(buyerUid: Int): BuyDataResponse
    fun sell(sellData: SellOrEditDataRequest): Boolean
    fun edit(editData: SellOrEditDataRequest): Boolean
    fun cancel(cancelData: CancelDataRequest): Boolean
    
    fun getConfig(): List<MarketItemConfig>
    fun getMyItem(uid: Int): List<MyItemMarket>
}

// Request market data
data class OrderDataRequest(
    val buyerUid: Int,
    val itemId: Int,
    val quantity: Int,
    val expiration: Int,
    var userGem: Float = 0F,
)

data class SellOrEditDataRequest(
    val sellerUid: Int,
    val itemId: Int,
    val itemType: Int,
    val quantity: Int,
    val oldQuantity: Int,
    val price: Double,
    val oldPrice: Double,
    var listId: List<Int>,
    val expiration: Int,
    val modifyDate: Long
)

data class CancelDataRequest(
    val sellerUid: Int,
    val itemId: Int,
    val price: Float,
    val itemType: Int,
    val expiration: Int
)

// Response market data
@Serializable
data class OrderDataResponse(
    val isOrderSuccess: Boolean,
    val totalQuantity: Int,
    val totalPrice: Int
)

@Serializable
data class BuyDataResponse(
    val itemId: Int,
    val itemType: Int,
    val fixedQuantity: Int,
    val totalQuantity: Int,
    val expiration: Int,
)