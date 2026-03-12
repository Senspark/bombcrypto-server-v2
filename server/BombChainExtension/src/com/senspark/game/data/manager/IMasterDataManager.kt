package com.senspark.game.data.manager

import com.senspark.common.service.IServerService
import com.senspark.common.service.IService
import com.senspark.game.data.model.config.MarketItemConfig
import com.smartfoxserver.v2.entities.data.ISFSObject

interface IMasterDataManager : IService, IServerService {
    override fun destroy() {}
    fun getGameConfig(clientBuildVersion: Int): ISFSObject
    fun getOnBoardingConfig(): Map<Int, Float>
}