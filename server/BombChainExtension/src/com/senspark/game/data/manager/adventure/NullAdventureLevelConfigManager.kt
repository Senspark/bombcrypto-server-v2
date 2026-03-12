package com.senspark.game.data.manager.adventure

import com.senspark.game.data.model.config.LevelStrategy
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.SFSArray

class NullAdventureLevelConfigManager : IAdventureLevelConfigManager {

    override fun initialize() {
    }

    override fun getStrategy(stage: Int, level: Int): LevelStrategy {
        throw CustomException("Feature not support")
    }

    override fun getNextLevel(stage: Int, level: Int): LevelStrategy? {
        return null
    }

    override fun getLevelMap(): SFSArray {
        return SFSArray()
    }
}