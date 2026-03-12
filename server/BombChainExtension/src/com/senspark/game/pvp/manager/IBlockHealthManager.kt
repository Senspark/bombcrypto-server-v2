package com.senspark.game.pvp.manager

import com.senspark.game.pvp.entity.BlockType

interface IBlockHealthManager {
    fun getHealth(type: BlockType): Int
}