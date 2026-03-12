package com.senspark.game.pvp.data

import com.senspark.game.pvp.info.IFallingBlockInfo

interface IFallingBlockData {
    val matchId: String
    val blocks: List<IFallingBlockInfo>
}