package com.senspark.game.pvp.strategy.state

import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.delta.BombStateDelta
import com.senspark.game.pvp.delta.IBombStateDelta
import com.senspark.game.pvp.entity.BombReason
import com.senspark.game.pvp.entity.BombState
import com.senspark.game.pvp.manager.IBombManagerState

class DefaultBombStateComparer(
    private val _logger: ILogger,
) : IBombStateComparer {
    companion object {
        private val deadBombState = BombState(
            isAlive = false,
            slot = 0,
            reason = BombReason.Null,
            x = 0f,
            y = 0f,
            range = 0,
            damage = 0,
            piercing = false,
            explodeDuration = 0,
            explodeRanges = emptyMap(),
            plantTimestamp = 0,
        )
    }

    override fun compare(
        state: IBombManagerState,
        lastState: IBombManagerState,
    ): List<IBombStateDelta> {
        val data = mutableListOf<BombStateDelta>()
        val keys = state.bombs.keys + lastState.bombs.keys
        keys.forEach { id ->
            val itemState = state.bombs[id] ?: deadBombState
            val lastItemState = lastState.bombs[id] ?: deadBombState
            val delta = BombStateDelta.compare(id, itemState, lastItemState) ?: return@forEach
            data.add(delta)
        }
        return data
    }
}