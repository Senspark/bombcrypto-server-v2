package com.senspark.game.manager.market

import com.senspark.common.service.IServerService
import com.senspark.common.service.IService
import com.senspark.game.data.model.config.MarketItemConfig
import com.smartfoxserver.v2.entities.data.ISFSObject

interface IMarketManager : IService, IServerService  {
    fun getMarketConfig(): List<MarketItemConfig>
    fun refreshMinPrice()
    fun getCurrentMinPrice(): ISFSObject
}