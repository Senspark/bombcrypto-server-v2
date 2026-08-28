package com.senspark.game.api

import com.senspark.game.data.model.config.MarketItemConfig
import com.senspark.game.data.model.config.MyItemMarket
import com.senspark.game.exception.CustomException

class NullMarketApi : IMarketApi {

    override fun initialize() {
    }

    override fun order(orderData: OrderDataRequest): OrderDataResponse {
        throw CustomException("Feature not support")
    }

    override fun cancelOrder(buyerUid: Int): Boolean {
        throw CustomException("Feature not support")
    }

    override fun buy(buyerUid: Int): BuyDataResponse {
        throw CustomException("Feature not support")
    }

    override fun sell(sellData: SellOrEditDataRequest): Boolean {
        throw CustomException("Feature not support")
    }

    override fun edit(editData: SellOrEditDataRequest): Boolean {
        throw CustomException("Feature not support")
    }

    override fun cancel(cancelData: CancelDataRequest): Boolean {
        throw CustomException("Feature not support")
    }

    override fun getConfig(): List<MarketItemConfig> {
        return emptyList()
    }

    override fun getMyItem(uid: Int): List<MyItemMarket> {
        return emptyList()
    }
}
