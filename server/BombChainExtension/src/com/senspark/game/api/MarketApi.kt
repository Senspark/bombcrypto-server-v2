package com.senspark.game.api

import com.senspark.common.utils.ILogger
import com.senspark.game.data.model.config.MarketItemConfig
import com.senspark.game.data.model.config.MyItemMarket
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.IEnvManager
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import kotlinx.serialization.json.put

class MarketApi(
    private val _envManager: IEnvManager,
    private val _logger: ILogger
) : IMarketApi {

    private var _jwt: String = ""

    override fun initialize() {
        _jwt = _envManager.apLoginToken
    }

    override fun order(orderData: OrderDataRequest): OrderDataResponse {
        try {
            val url = "${_envManager.apMarketPath}/order"
            val body = buildJsonObject {
                put("buyerUid", orderData.buyerUid)
                put("itemId", orderData.itemId)
                put("quantity", orderData.quantity)
                put("expiration", orderData.expiration)
                put("userGem", orderData.userGem)
            }
            val response = StandardSensparkApi.post(url, _jwt, body, OrderDataResponse.serializer(), _logger)
            return response!!
        }
        catch (e: ApiException) {
            _logger.error("Api exception in MarketApi order: ${e.message}")
            handleMarketException(e)
        }
        catch (e: Exception) {
            _logger.error("Error in MarketApi order: ${e.message}")
            throw e
        }
    }

    override fun cancelOrder(buyerUid: Int): Boolean {
        try {
            val url = "${_envManager.apMarketPath}/cancel_order"
            val body = buildJsonObject {
                put("buyerUid", buyerUid)
            }
            val response = StandardSensparkApi.post(url, _jwt, body, Boolean.serializer(), _logger)
            return response!!
        }
        catch (e: ApiException) {
            _logger.error("Api exception in MarketApi cancel order: ${e.message}")
            handleMarketException(e)
        }
        catch (e: Exception) {
            _logger.error("Error in MarketApi cancel order: ${e.message}")
            throw e
        }
    }

    override fun buy(buyerUid: Int): BuyDataResponse {
        try {
            val url = "${_envManager.apMarketPath}/buy"
            val body = buildJsonObject {
                put("buyerUid", buyerUid)
            }
            val response = StandardSensparkApi.post(url, _jwt, body, BuyDataResponse.serializer(), _logger)
            return response!!
        }
        catch (e: ApiException) {
            _logger.error("Api exception in MarketApi buy: ${e.message}")
            handleMarketException(e)
        }
        catch (e: Exception) {
            _logger.error("Error in MarketApi buy: ${e.message}")
            throw e
        }
    }

    override fun sell(sellData: SellOrEditDataRequest): Boolean {
        try {
            val url = "${_envManager.apMarketPath}/sell"
            val body = buildJsonObject {
                put("sellerUid", sellData.sellerUid)
                put("itemId", sellData.itemId)
                put("quantity", sellData.quantity)
                put("price", sellData.price)
                put("listId", JsonArray(sellData.listId.map { JsonPrimitive(it) }))
                put("expiration", sellData.expiration)
                put("modifyDate", sellData.modifyDate)
            }
            val response = StandardSensparkApi.post(url, _jwt, body, Boolean.serializer(), _logger)
            return response!!
        }
        catch (e: ApiException) {
            _logger.error("Api exception in MarketApi sell: ${e.message}")
            handleMarketException(e)
        }
        catch (e: Exception) {
            _logger.error("Error in MarketApi sell: ${e.message}")
            throw e
        }
    }
    override fun edit(editData: SellOrEditDataRequest): Boolean {
        try {
            val url = "${_envManager.apMarketPath}/edit"
            val body = buildJsonObject {
                put("sellerUid", editData.sellerUid)
                put("itemId", editData.itemId)
                put("quantity", editData.quantity)
                put("price", editData.price)
                put("oldPrice", editData.oldPrice)
                put("listId", JsonArray(editData.listId.map { JsonPrimitive(it) }))
                put("expiration", editData.expiration)
                put("modifyDate", editData.modifyDate)
            }
            val response = StandardSensparkApi.post(url, _jwt, body, Boolean.serializer(), _logger)
            return response!!
        }
        catch (e: ApiException) {
            _logger.error("Api exception in MarketApi edit: ${e.message}")
            handleMarketException(e)
        }
        catch (e: Exception) {
            _logger.error("Error in MarketApi edit: ${e.message}")
            throw e
        }
    }

    override fun cancel(cancelData: CancelDataRequest): Boolean {
        try {
            val url = "${_envManager.apMarketPath}/cancel"
            val body = buildJsonObject {
                put("sellerUid", cancelData.sellerUid)
                put("itemId", cancelData.itemId)
                put("price", cancelData.price)
                put("expiration", cancelData.expiration)
            }
            val response = StandardSensparkApi.post(url, _jwt, body, Boolean.serializer(), _logger)
            return response!!
        }
        catch (e: ApiException) {
            _logger.error("Api exception in MarketApi cancel: ${e.message}")
            handleMarketException(e)
        }
        catch (e: Exception) {
            _logger.error("Error in MarketApi order: ${e.message}")
            throw e
        }
    }
    
    override fun getConfig(): List<MarketItemConfig> {
        try {
            val url = "${_envManager.apMarketPath}/get_config"
            val response = StandardSensparkApi.get(url, _jwt, ListSerializer(MarketItemConfig.serializer()), _logger)
            return response ?: emptyList()
        } catch (e: ApiException) {
            _logger.error("Api exception in MarketApi get config: ${e.message}")
            return emptyList()
        } catch (e: Exception) {
            _logger.error("Error in MarketApi get config: ${e.message}")
            return emptyList()
        }
    }

    override fun getMyItem(uid: Int): List<MyItemMarket> {
        try {
            val url = "${_envManager.apMarketPath}/get_my_item"
            val body = buildJsonObject {
                put("uid", uid)
            }
            val response = StandardSensparkApi.post(url, _jwt, body, ListSerializer(MyItemMarket.serializer()), _logger)
            return response ?: emptyList()
        } catch (e: ApiException) {
            _logger.error("Api exception in MarketApi get my item: ${e.message}")
            return emptyList()
        } catch (e: Exception) {
            _logger.error("Error in MarketApi get my item: ${e.message}")
            return emptyList()
        }
    }


    private fun handleMarketException(e: ApiException): Nothing {
        val jsonStartIndex = e.message?.indexOf("{") ?: -1
        val jsonString = if (jsonStartIndex != -1) e.message?.substring(jsonStartIndex) else "{}"
        
        val json = Json.parseToJsonElement(jsonString ?: "{}").jsonObject
        val errorMessage = json["message"]?.jsonPrimitive?.content ?: "Unknown error"
        val errorCode = json["code"]?.jsonPrimitive?.int

        if (isExpectedErrorCode(errorCode)) {
            throw CustomException(errorMessage)
        }
        throw e
    }

    private fun isExpectedErrorCode(errorCode: Int?): Boolean {
        // Unexpected error
        if (errorCode == null || errorCode == 6000) {
            return false
        }
        if (errorCode in 6001..6010) {
            return true
        }
        return false
    }
}

