package com.senspark.game.pvp.info

import com.senspark.game.pvp.entity.BlockType

interface IBlockInfo {
    val type: BlockType
    val x: Int
    val y: Int
    val health: Int
}