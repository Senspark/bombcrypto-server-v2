package com.senspark.game.pvp.data

import com.senspark.game.pvp.delta.IBlockStateDelta
import com.senspark.game.pvp.delta.IBombStateDelta
import com.senspark.game.pvp.delta.IHeroStateDelta

interface IMatchObserveData {
    val id: Int
    val timestamp: Long
    val matchId: String
    val heroDelta: List<IHeroStateDelta>
    val bombDelta: List<IBombStateDelta>
    val blockDelta: List<IBlockStateDelta>
}