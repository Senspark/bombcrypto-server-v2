package com.senspark.game.pvp.entity

interface IHeroEffectState {
    /** Whether this effect is active. */
    val isActive: Boolean

    /** Active/inactive reason. */
    val reason: HeroEffectReason

    /** Start timestamp (if is active). */
    val timestamp: Int

    /** Effect duration. */
    val duration: Int

    fun encode(): Long
}