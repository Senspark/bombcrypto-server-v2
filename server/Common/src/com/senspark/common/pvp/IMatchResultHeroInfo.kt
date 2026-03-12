package com.senspark.common.pvp

interface IMatchResultHeroInfo {
    /** Hero ID. */
    val id: Int

    /** Last damaged source. */
    val damageSource: Int

    /** Collected rewards in-game. */
    val rewards: Map<Int, Float>

    /** Collected item (booster) in-game. */
    val collectedItems: List<Int>
}