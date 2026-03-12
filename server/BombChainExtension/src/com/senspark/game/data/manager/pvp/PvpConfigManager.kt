package com.senspark.game.data.manager.pvp

import com.senspark.common.utils.ILogger
import com.senspark.game.api.PVPConfig
import com.senspark.game.api.PVPConfigsApi
import com.senspark.game.manager.IEnvManager

class PvpConfigManager(
    envManager: IEnvManager,
    logger: ILogger,
) : IPvpConfigManager {
    private val _api = PVPConfigsApi(envManager, logger)
    private lateinit var _config: PVPConfig

    override fun initialize() {
        _config = _api.getConfig()
    }

    override fun destroy() = Unit

    override fun getConfig(): PVPConfig {
        return _config
    }

    override fun reloadConfig() {
        _config = _api.getConfig()
    }
}