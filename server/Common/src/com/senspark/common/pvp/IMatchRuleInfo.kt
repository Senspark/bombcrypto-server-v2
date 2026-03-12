package com.senspark.common.pvp

interface IMatchRuleInfoClient {
    /** Number of users. */
    val room_size: Int

    /** Number of users per team. */
    val team_size: Int

    /** Number of rounds (e.g. bo1, bo3, bo5). */
    val round: Int

    /** Whether a draw result is allowed. */
    val can_draw: Boolean

    val is_tournament: Boolean
}

interface IMatchRuleInfo {
    /** Number of users. */
    val roomSize: Int

    /** Number of users per team. */
    val teamSize: Int

    /** Number of rounds (e.g. bo1, bo3, bo5). */
    val round: Int

    /** Whether a draw result is allowed. */
    val canDraw: Boolean

    val isTournament: Boolean
}