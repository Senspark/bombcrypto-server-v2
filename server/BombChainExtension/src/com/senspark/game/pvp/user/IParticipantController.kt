package com.senspark.game.pvp.user

import com.senspark.common.pvp.IMatchUserInfo
import com.senspark.game.constant.Booster

interface IParticipantController : IUserController {
    /** User info. */
    val info: IMatchUserInfo

    val teamId: Int

    /** Whether this user is ready. */
    val isReady: Boolean

    val isQuited: Boolean

    /** Gets used boosters. */
    val usedBoosters: Map<Int, Int>

    /** Readies this player. */
    fun ready()

    fun quit()

    /**
     * Attempts to use the specified booster item.
     * @param booster The desired booster.
     */
    fun useBooster(booster: Booster)

    /**
     * Resets ready state and booster cooldown.
     */
    fun reset()
}