package com.senspark.game.api

import com.senspark.common.pvp.IMatchRuleInfo
import com.senspark.common.pvp.IMatchRuleInfoClient
import com.senspark.common.pvp.IMatchTeamInfo
import com.senspark.common.pvp.PvpMode

interface IPvpResultUserInfoClient {
    /** Original server ID. */
    var server_id: String

    var is_bot: Boolean

    var team_id: Int

    /** User ID, used for tracking, etc. */
    var user_id: Int

    val username: String

    /** Final bomb rank in the current season. */
    val rank: Int

    /** Final pvp point in the current season. */
    val point: Int

    /** Final match count in the current season. */
    var match_count: Int

    /** Final winning match count in the current season. */
    var win_match_count: Int

    /** Point change. */
    var delta_point: Int

    /** Used boosters in-game (key, shield). */
    var used_boosters: Map<Int, Int>

    val quit: Boolean

    /** Hero ID. */
    val heroId: Int

    /** Last damaged source. */
    val damageSource: Int

    /** Collected rewards in-game. */
    val rewards: Map<Int, Float>

    /** Collected item (booster) in-game. */
    val collectedItems: List<Int>

    /** Final ranking in match (1st, 2nd, 3rd...). */
    val ranking: Int
}

interface IPvpResultInfoClient {
    /** Match ID. */
    val id: String
    val serverId: String
    val timestamp: Long

    /** mode. */
    val mode: PvpMode

    /** Whether this match is draw. */
    var is_draw: Boolean

    /** Winning team slot (if not draw). */
    var winning_team: Int

    /** Team scores. */
    val scores: List<Int>

    val duration: Int

    /** Match rule. */
    val rule: IMatchRuleInfoClient

    val team: List<IMatchTeamInfo>

    /** User info. */
    val info: List<IPvpResultUserInfoClient>

    val wagerMode: Int
    val wagerTier: Int
    val wagerToken: Int
    var signature: String?
    var integrityLogs: String?
}

interface IPvpResultUserInfo {
    /** Original server ID. */
    val serverId: String

    val isBot: Boolean
    
    val teamId: Int

    /** User ID, used for tracking, etc. */
    val userId: Int

    val username: String

    /** Final bomb rank in the current season. */
    val rank: Int

    /** Final pvp point in the current season. */
    val point: Int

    /** Final match count in the current season. */
    val matchCount: Int

    /** Final winning match count in the current season. */
    val winMatchCount: Int

    /** Point change. */
    val deltaPoint: Int

    /** Used boosters in-game (key, shield). */
    val usedBoosters: Map<Int, Int>

    val quit: Boolean

    /** Hero ID. */
    val heroId: Int

    /** Last damaged source. */
    val damageSource: Int

    /** Collected rewards in-game. */
    val rewards: Map<Int, Float>

    /** Collected item (booster) in-game. */
    val collectedItems: List<Int>

    /** Final ranking in match (1st, 2nd, 3rd...). */
    val ranking: Int
}

interface IPvpResultInfo {
    /** Match ID. */
    val id: String
    val serverId: String
    val timestamp: Long

    /** mode. */
    val mode: PvpMode

    /** Whether this match is draw. */
    val isDraw: Boolean

    /** Winning team slot (if not draw). */
    val winningTeam: Int

    /** Team scores. */
    val scores: List<Int>

    val duration: Int

    /** Match rule. */
    val rule: IMatchRuleInfo

    val team: List<IMatchTeamInfo>

    /** User info. */
    val info: List<IPvpResultUserInfo>

    val wagerMode: Int
    val wagerTier: Int
    val wagerToken: Int
    val signature: String?
    val integrityLogs: String?
}