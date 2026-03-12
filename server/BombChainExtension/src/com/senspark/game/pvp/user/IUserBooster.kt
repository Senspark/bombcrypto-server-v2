package com.senspark.game.pvp.user

import com.senspark.game.constant.Booster

interface IUserBooster {
    /** Unique ID. */
    val item: Booster

    /** Item cooldown (in milliseconds). */
    val cooldown: Int

    /** How many times this booster was used. */
    val useTimes: Int

    /** Last used timestamp. */
    val usedTimestamp: Long

    /** Attempts to use this booster. */
    fun use()
}