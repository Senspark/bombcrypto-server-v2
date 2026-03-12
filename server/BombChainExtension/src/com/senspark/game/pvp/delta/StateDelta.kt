package com.senspark.game.pvp.delta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class StateDelta<T>(
    @SerialName("state") val state: T,
    @SerialName("last_state") val lastState: T,
) {
    companion object {
        fun <T> compare(state: T, lastState: T): StateDelta<T>? {
            return if (state == lastState) null else StateDelta(state, lastState)
        }
    }
}