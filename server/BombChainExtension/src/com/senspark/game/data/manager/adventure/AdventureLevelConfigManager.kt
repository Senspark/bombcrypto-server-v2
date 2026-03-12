package com.senspark.game.data.manager.adventure

import com.senspark.game.data.model.config.LevelStrategy
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class AdventureLevelConfigManager(
    private val _shopDataAccess: IShopDataAccess,
) : IAdventureLevelConfigManager {

    private val _levelStrategies: MutableMap<Int, Map<Int, LevelStrategy>> = mutableMapOf()

    override fun initialize() {
        _levelStrategies.putAll(_shopDataAccess.loadAdventureLevelStrategy())
    }

    override fun getStrategy(stage: Int, level: Int): LevelStrategy {
        return _levelStrategies[stage]?.get(level) ?: throw CustomException("Level not found", ErrorCode.SERVER_ERROR)
    }

    override fun getNextLevel(stage: Int, level: Int): LevelStrategy? {
        _levelStrategies[stage]?.let {
            it[level + 1]?.let { it2 -> return it2 }
        }
        _levelStrategies[stage + 1]?.let {
            it[1]?.let { it2 -> return it2 }
        }
        return null
    }

    override fun getLevelMap(): SFSArray {
        val sfsArray = SFSArray()
        _levelStrategies.forEach {
            val sfsObject = SFSObject()
            sfsObject.putInt("stage", it.key)
            sfsObject.putInt("level_count", it.value.size)
            sfsArray.addSFSObject(sfsObject)
        }
        return sfsArray
    }
}