package com.senspark.game.pvp.strategy.state

import com.senspark.game.pvp.delta.BlockStateDelta
import com.senspark.game.pvp.delta.IBlockStateDelta
import com.senspark.game.pvp.entity.BlockReason
import com.senspark.game.pvp.entity.BlockState
import com.senspark.game.pvp.entity.BlockType
import com.senspark.game.pvp.manager.IMapManagerState

class DefaultMapStateComparer : IMapStateComparer {
    companion object {
        // Internal usage.
        private val deadBlockState = BlockState(
            isAlive = false,
            reason = BlockReason.Null,
            type = BlockType.Null,
            health = 0,
            maxHealth = 0,
        )
    }

    override fun compare(
        state: IMapManagerState,
        lastState: IMapManagerState,
    ): List<IBlockStateDelta> {
        val data = mutableListOf<BlockStateDelta>()
        val keys = state.blocks.keys + lastState.blocks.keys
        keys.forEach { key ->
            val itemState = state.blocks[key] ?: deadBlockState
            val lastItemState = lastState.blocks[key] ?: deadBlockState
            val (x, y) = key
            val delta = BlockStateDelta.compare(x, y, itemState, lastItemState) ?: return@forEach
            data.add(delta)
        }
        return data
    }
}