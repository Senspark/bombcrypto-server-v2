package com.senspark.common.pvp

interface IMatchUserInfo {
    /** Current logged in server. */
    val serverId: String

    /** Client data. */
    val buildVersion: Int
    val matchId: String
    val mode: Int
    val isTest: Boolean
    val isWhitelisted: Boolean
    val isBot: Boolean

    /** User ID, used for tracking, etc. */
    val userId: Int

    /** Username. */
    val username: String

    /** Display name. */
    val displayName: String

    /** Total pvp match count. */
    val totalMatchCount: Int

    /** Pvp match count in the current season */
    val matchCount: Int

    /** Pvp winning match count in the current season. */
    val winMatchCount: Int

    /** Ranking in the current season. */
    val rank: Int

    /** Ranking point in the current season. */
    val point: Int

    /** Enabled boosters in join scene. */
    val boosters: List<Int>

    /** List of usable boosters in-game. */
    val availableBoosters: Map<Int, Int>

    /** Hero info. */
    val hero: IMatchHeroInfo
    
    val avatar: Int?
}