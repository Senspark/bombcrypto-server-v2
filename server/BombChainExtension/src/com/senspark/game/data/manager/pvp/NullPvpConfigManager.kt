package com.senspark.game.data.manager.pvp

import com.senspark.game.api.PVPConfig
import com.senspark.game.exception.CustomException

class NullPvpConfigManager : IPvpConfigManager {

    override fun initialize() {
    }
    
    override fun destroy() = Unit

    override fun getConfig(): PVPConfig {
        throw CustomException("Feature not support")
    }

    override fun reloadConfig() {}
}