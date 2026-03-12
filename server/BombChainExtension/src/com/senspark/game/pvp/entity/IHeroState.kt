package com.senspark.game.pvp.entity

interface IHeroBaseState : IEntityState {
    val health: Int

    /** Last damage source. */
    val damageSource: HeroDamageSource

    /** Collected items. */
    val items: Map<HeroItem, Int>

    /** Effects states. */
    val effects: Map<HeroEffect, IHeroEffectState>

    fun encode(): List<Long>
}

interface IHeroPositionState {
    /** Horizontal position. */
    val x: Float

    /** Vertical position. */
    val y: Float

    /** Facing direction. */
    val direction: Direction

    fun encode(): Long
}

interface IHeroState : IEntityState {
    val baseState: IHeroBaseState?
    val positionState: IHeroPositionState?
}