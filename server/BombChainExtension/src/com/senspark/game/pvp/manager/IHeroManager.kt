package com.senspark.game.pvp.manager

import com.senspark.game.pvp.entity.IHero
import com.senspark.game.pvp.entity.IHeroState

interface IHeroManagerState {
    /** States of active heroes. */
    val heroes: Map<Int, IHeroState>

    fun apply(state: IHeroManagerState): IHeroManagerState
    fun encode(): List<Long>
}

interface IHeroManager : IUpdater {
    /** Gets the current state. */
    val state: IHeroManagerState

    /** Applies the specified state. */
    fun applyState(state: IHeroManagerState)

    /** Gets the hero at the specified slot. */
    fun getHero(slot: Int): IHero

    fun damageBomb(x: Int, y: Int, amount: Int)
    fun damageFallingBlock(x: Int, y: Int)
}