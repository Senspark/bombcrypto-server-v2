package com.senspark.game.pvp.delta

import com.senspark.game.pvp.entity.IBlockState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class BlockStateDelta(
    @SerialName("x") override val x: Int,
    @SerialName("y") override val y: Int,
    @SerialName("state") override val state: Long,
    @SerialName("last_state") override val lastState: Long,
) : IBlockStateDelta {
    companion object {
        fun compare(
            x: Int,
            y: Int,
            state: IBlockState,
            lastState: IBlockState,
        ): BlockStateDelta? {
            val encodedState = state.encode()
            val encodedLastState = lastState.encode()
            if (encodedState == encodedLastState) {
                return null
            }
            return BlockStateDelta(
                x = x,
                y = y,
                state = encodedState,
                lastState = encodedLastState,
            )
        }
    }
}