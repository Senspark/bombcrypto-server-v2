package com.senspark.game.pvp.delta

import com.senspark.game.pvp.entity.IBombState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class BombStateDelta(
    @SerialName("id") override val id: Int,
    @SerialName("state") override val state: List<Long>,
    @SerialName("last_state") override val lastState: List<Long>,
) : IBombStateDelta {
    companion object {
        fun compare(
            id: Int,
            state: IBombState,
            lastState: IBombState,
        ): BombStateDelta? {
            val encodedState = state.encode()
            val encodedLastState = lastState.encode()
            if (encodedState == encodedLastState) {
                return null
            }
            return BombStateDelta(
                id = id,
                state = encodedState,
                lastState = encodedLastState,
            )
        }
    }
}