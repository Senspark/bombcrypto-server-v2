package com.senspark.game.data.manager

import com.senspark.game.data.model.config.MarketItemConfig
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class NullMasterDataManager : IMasterDataManager {

    override fun initialize() {
    }

    override fun getGameConfig(clientBuildVersion: Int): ISFSObject {
        return SFSObject()
    }

    override fun getOnBoardingConfig(): Map<Int, Float> {
        return emptyMap()
    }
    
}