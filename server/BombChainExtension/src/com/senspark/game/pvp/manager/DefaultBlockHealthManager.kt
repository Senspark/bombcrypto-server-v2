package com.senspark.game.pvp.manager

import com.senspark.game.data.PvPBlockHealthData
import com.senspark.game.pvp.entity.BlockType

class DefaultBlockHealthManager(data: List<PvPBlockHealthData>) : IBlockHealthManager {
    private val _data = data.associateBy({ it.blockType }, { it.value })
    override fun getHealth(type: BlockType): Int {
        return _data[type] ?: throw Exception("Could not find block health: $type")
    }
}