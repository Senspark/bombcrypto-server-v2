package com.senspark.game.pvp.delta

import com.senspark.game.pvp.entity.IHeroState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class HeroStateDelta(
    @SerialName("slot") override val slot: Int,
    @SerialName("base") override val base: StateDelta<List<Long>>? = null,
    @SerialName("position") override val position: StateDelta<Long>? = null,
) : IHeroStateDelta {
    companion object {
        fun compare(
            slot: Int,
            state: IHeroState,
            lastState: IHeroState,
        ): HeroStateDelta? {
            val baseState = state.baseState
            val lastBaseState = lastState.baseState
            require(baseState != null && lastBaseState != null) { "Base state is null" }
            val positionState = state.positionState
            val lastPositionState = lastState.positionState
            require(positionState != null && lastPositionState != null) { "Base state is null" }
            val baseDelta = StateDelta.compare(baseState.encode(), lastBaseState.encode())
            val positionDelta = StateDelta.compare(positionState.encode(), lastPositionState.encode())
            if (baseDelta == null && positionDelta == null) {
                return null
            }
            return HeroStateDelta(
                slot = slot,
                base = baseDelta,
                position = positionDelta,
            )
        }
    }
}