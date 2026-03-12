package com.senspark.game.pvp.info

import com.senspark.common.pvp.*
import com.smartfoxserver.v2.util.MD5
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MatchInfoClient(
    @SerialName("id") val id: String,
    @SerialName("server_id") val serverId: String,
    @SerialName("server_detail") val serverDetail: String? = null,
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("mode") val mode: PvpMode,
    @SerialName("rule") val rule: MatchRuleInfoClient,
    @SerialName("team") val team: List<IMatchTeamInfo>,
    @SerialName("slot") val slot: Int,
    @SerialName("info") val info: List<IMatchUserInfo>,
) {
    fun toMatchInfo(): MatchInfo {
        return MatchInfo(
            id = id,
            serverId = serverId,
            serverDetail = serverDetail?: "",
            timestamp = timestamp,
            mode = mode,
            rule = MatchRuleInfo(
                roomSize = rule.room_size,
                teamSize = rule.team_size,
                canDraw = rule.can_draw,
                round = rule.round,
                isTournament = rule.is_tournament
            ),
            team = team,
            slot = slot,
            info = info,
        )

    }
}

@Serializable
class MatchInfo(
    @SerialName("id") override val id: String,
    @SerialName("server_id") override val serverId: String,
    @SerialName("server_detail") override val serverDetail: String,
    @SerialName("timestamp") override val timestamp: Long,
    @SerialName("mode") override val mode: PvpMode,
    @SerialName("rule") override val rule: IMatchRuleInfo,
    @SerialName("team") override val team: List<IMatchTeamInfo>,
    @SerialName("slot") override val slot: Int,
    @SerialName("info") override val info: List<IMatchUserInfo>,
) : IMatchInfo {
    companion object {
        const val PROPERTY_KEY = "JOIN_PVP_MATCH_INFO"
        const val SALT = "e1bf721643237e2e62219c21f76e80c9"
    }

    private val _hash: String = MD5.getInstance()
        .getHash("$this-$SALT")

    override val hash get() = _hash

    override fun toString(): String {
        return "$id-$serverId-$timestamp-$mode-$rule-$team-$slot-$info"
    }
}
