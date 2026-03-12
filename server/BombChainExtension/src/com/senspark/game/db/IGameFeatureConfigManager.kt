package com.senspark.game.db

import com.senspark.common.service.IServerService

interface IGameFeatureConfigManager : IServerService {
    fun getDisableFeaturesByVersion(version: Int): List<Int>
}