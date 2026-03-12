package com.senspark.common.pvp

import com.senspark.common.constant.ItemId

interface IMatchHeroInfo {
    /** Hero ID. */
    val id: Int

    /** Appearance stats. */
    val color: Int
    val skin: Int
    val skinChests: Map<Int, List<ItemId>>

    /** Base stats. */
    val health: Int
    val speed: Int
    val damage: Int
    val bombCount: Int
    val bombRange: Int
    val maxHealth: Int
    val maxSpeed: Int
    val maxDamage: Int
    val maxBombCount: Int
    val maxBombRange: Int
}