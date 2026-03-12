package com.senspark.game.pvp.strategy.state

import com.senspark.game.pvp.delta.HeroStateDelta
import com.senspark.game.pvp.delta.IHeroStateDelta
import com.senspark.game.pvp.entity.Direction
import com.senspark.game.pvp.entity.HeroDamageSource
import com.senspark.game.pvp.entity.HeroState
import com.senspark.game.pvp.manager.IHeroManagerState

class DefaultHeroStateComparer : IHeroStateComparer {
    companion object {
        // Internal usage.
        private val deadHeroState = HeroState(
            isAlive = false,
            x = 0f,
            y = 0f,
            direction = Direction.Down,
            health = 0,
            damageSource = HeroDamageSource.Null,
            items = emptyMap(),
            effects = emptyMap(),
        )
    }

    override fun compare(
        state: IHeroManagerState,
        lastState: IHeroManagerState,
    ): List<IHeroStateDelta> {
        require(state.heroes.keys == lastState.heroes.keys) { "Hero state mismatched" }
        val data = mutableListOf<HeroStateDelta>()
        val keys = state.heroes.keys
        keys.forEach { slot ->
            val itemState = state.heroes[slot] ?: deadHeroState
            val lastItemState = lastState.heroes[slot] ?: deadHeroState
            val delta = HeroStateDelta.compare(slot, itemState, lastItemState) ?: return@forEach
            data.add(delta)
        }
        return data
    }
}