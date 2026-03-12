package com.senspark.game.data.manager.pvp

import com.senspark.common.service.IServerService
import com.senspark.common.service.IService
import com.senspark.game.api.PVPConfig

interface IPvpConfigManager : IService, IServerService {
    fun getConfig(): PVPConfig
    fun reloadConfig()
}