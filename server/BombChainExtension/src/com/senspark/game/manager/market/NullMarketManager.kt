package com.senspark.game.manager.market

import com.senspark.game.data.model.config.MarketItemConfig
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class NullMarketManager : IMarketManager {

    override fun initialize() {
    }

    override fun getMarketConfig(): List<MarketItemConfig> {
        return emptyList()
    }

    override fun refreshMinPrice() {
    }

    override fun getCurrentMinPrice(): ISFSObject {
        return SFSObject()
    }

    override fun destroy() {
    }
}
