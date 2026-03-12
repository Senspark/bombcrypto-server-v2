package com.senspark.game.manager.market

import com.senspark.common.cache.ICacheService
import com.senspark.common.utils.ILogger
import com.senspark.game.api.IMarketApi
import com.senspark.game.constant.CachedKeys
import com.senspark.game.data.model.config.MarketItemConfig
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.json.Json

class MarketManager(
    private val redis: ICacheService,
    private val marketApi: IMarketApi,
    private val logger: ILogger
) : IMarketManager {
    private lateinit var _marketConfig: List<MarketItemConfig>
    private var _currentMinPrice = mutableMapOf<Int, Float>()
    private var _currentMinPriceSfs: ISFSObject = SFSObject()
    
    override fun initialize() {
        _marketConfig = marketApi.getConfig()
    }
    
    override fun getMarketConfig(): List<MarketItemConfig> {
        // Đảm bảo có config nếu lúc khởi động server gọi api ko có config
        if (!::_marketConfig.isInitialized || _marketConfig.isEmpty()) {
            _marketConfig = marketApi.getConfig()
        }
        return _marketConfig
    }

    override fun refreshMinPrice() {
        try {

            val response = redis.get(CachedKeys.MARKET_MIN_PRICE)
            if (response != null) {
                _currentMinPrice = Json.decodeFromString<Map<Int, Float>>(response)
                    .toMap()
                    .toMutableMap()
            }
            _currentMinPriceSfs = parseToSfs(_currentMinPrice)
        } catch (e: Exception) {
            logger.error("Error while refreshing min price: ${e.message}")
        }
    }
    
    private fun parseToSfs(data: Map<Int, Float>): ISFSObject {
        val sfsObject = SFSObject()
        val array = SFSArray()
        data.entries.forEach { entry ->
            val itemSfs = SFSObject()
            itemSfs.putInt("item_id", entry.key)
            itemSfs.putFloat("min_price", entry.value)
            array.addSFSObject(itemSfs)
        }
        sfsObject.putSFSArray("data", array)
        return sfsObject
    }

    override fun getCurrentMinPrice(): ISFSObject {
       return _currentMinPriceSfs
    }

    override fun destroy() {
        
    }
}