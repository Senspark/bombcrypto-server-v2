package com.senspark.common.pvp

interface IMatchResultUserInfo {
    /** Originally logged in server. */
    val serverId: String

    /** Client data. */
    val isTest: Boolean
    val isBot: Boolean

    val teamId: Int

    /** User ID, used for tracking, etc. */
    val userId: Int

    /** Username. */
    val username: String

    /** Pvp match count in the current season */
    val matchCount: Int

    /** Pvp winning match count in the current season. */
    val winMatchCount: Int

    /** Ranking point in the current season. */
    val point: Int

    /** Enabled boosters in join scene. */
    val boosters: List<Int>

    /** Used boosters in-game. */
    val usedBoosters: Map<Int, Int>

    /** Whether this user quited. */
    val quit: Boolean

    /** Hero info. */
    val hero: IMatchResultHeroInfo

    /** Final ranking in match (1st, 2nd, 3rd...). */
    val ranking: Int
}