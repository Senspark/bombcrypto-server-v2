package com.senspark.game.api.redis

import com.senspark.common.pvp.PvpMode
import kotlinx.serialization.Serializable

@Serializable
sealed interface IMatchRule {
    val roomSize: Int

    val teamSize: Int

    /** Whether a draw result is allowed. */
    val canDraw: Boolean

    /** Minimum number of rounds. */
    val round: Int

    val isTournament: Boolean
}


@Serializable
sealed interface IMatchTeam {
    val slots: List<Int>
}

@Serializable
sealed interface IUserData {
    val isBot: Boolean
    val displayName: String
    val boosters: IntArray
    val availableBoosters: Map<Int, Int>
    val avatar: Int
    // Other aux data.

    // Hero data.
    val hero: IPvpHeroInfo
}

@Serializable
sealed interface IUser {
    /** User ID. */
    val id: String

    /** Server ID (sa, sea). */
    val serverId: String

    /** Preferrable match ID. */
    val matchId: String?

    /** Match mode mask. */
    val mode: Int

    /** Total number played match (used to check play with bot). */
    val totalMatchCount: Int

    /** Pvp rank. */
    val rank: Int

    /** Pvp point. */
    val point: Int

    /** Matching timestamp. */
    val timestamp: Long

    /**
     * Thời gian này sẽ update lại mỗi khi user keep joining queue
     */
    val refreshTimestamp: Long

    /** Aux data, may vary. */
    val data: IUserData
}

@Serializable
sealed interface IMatch {
    /** Gets the match token id. */
    val id: String

    /** Gets the match zone. */
    val zone: String

    val serverDetail: String?

    val mode: PvpMode

    val rule: IMatchRule

    val team: List<IMatchTeam>

    /** Gets the users in this match. */
    val users: List<IUser>

    /** Gets the match timestamp. */
    val timestamp: Long
}

interface IRedisPvpFoundMatchApi {
    fun parse(data: String): IMatch
}
    
