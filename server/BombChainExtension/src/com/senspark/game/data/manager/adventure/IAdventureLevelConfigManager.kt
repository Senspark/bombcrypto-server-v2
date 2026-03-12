package com.senspark.game.data.manager.adventure

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.config.LevelStrategy
import com.smartfoxserver.v2.entities.data.SFSArray

interface IAdventureLevelConfigManager : IServerService {
    fun getStrategy(stage: Int, level: Int): LevelStrategy
    fun getNextLevel(stage: Int, level: Int): LevelStrategy?
    fun getLevelMap(): SFSArray
}