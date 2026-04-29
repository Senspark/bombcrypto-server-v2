package com.senspark.game.api.redis

import com.senspark.common.cache.IMessengerService
import com.senspark.common.pvp.PvpMode
import com.senspark.game.pvp.utility.JsonUtility
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MatchRule(
    override val roomSize: Int,
    override val teamSize: Int,
    override val canDraw: Boolean,
    override val round: Int,
    override val isTournament: Boolean
) : IMatchRule


@Serializable
class MatchTeam(
    override val slots: List<Int>
) : IMatchTeam

@Serializable
class UserData(
    override val isBot: Boolean,
    override val displayName: String,
    override val boosters: IntArray,
    override val availableBoosters: Map<Int, Int>,
    override val avatar: Int,
    // Other aux data.

    // Hero data.
    @SerialName("hero") override val hero: PvpHeroInfo,
    override val wagerMode: Int = 0,
    override val wagerTier: Int = 0,
    override val wagerToken: Int = 0
) : IUserData

@Serializable
class User(
    override val id: String,
    override val serverId: String,
    override val matchId: String?,
    override val mode: Int,
    override val totalMatchCount: Int,
    override val rank: Int,
    override val point: Int,
    override val timestamp: Long,
    override val refreshTimestamp: Long,
    override val data: UserData,
) : IUser

@Serializable
class Match(
    override val id: String,
    override val zone: String,
    override val mode: PvpMode,
    override val rule: MatchRule,
    override val team: List<MatchTeam>,
    override val users: List<User>,
    override val timestamp: Long,
    override val serverDetail: String? = "",
) : IMatch

class RedisPvpFoundMatchApi(messengerService: IMessengerService) : IRedisPvpFoundMatchApi {
    private val _json = JsonUtility.json
    private val _messengerService = messengerService

    override fun parse(data: String): IMatch {
        val match = _json.decodeFromString<Match>(data)
        return match
    }
}
