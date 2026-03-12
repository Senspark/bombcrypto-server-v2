package com.senspark.game.pvp.info

import com.senspark.common.pvp.IMatchHeroInfo
import com.senspark.common.pvp.IMatchUserInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MatchUserInfo(
    @SerialName("server_id") override val serverId: String,
    @SerialName("build_version") override val buildVersion: Int,
    @SerialName("match_id") override val matchId: String,
    @SerialName("mode") override val mode: Int,
    @SerialName("is_test") override val isTest: Boolean,
    @SerialName("is_whitelisted") override val isWhitelisted: Boolean,
    @SerialName("is_bot") override val isBot: Boolean,
    @SerialName("user_id") override val userId: Int,
    @SerialName("username") override val username: String,
    @SerialName("display_name") override val displayName: String,
    @SerialName("total_match_count") override val totalMatchCount: Int,
    @SerialName("match_count") override val matchCount: Int,
    @SerialName("win_match_count") override val winMatchCount: Int,
    @SerialName("rank") override val rank: Int,
    @SerialName("point") override val point: Int,
    @SerialName("boosters") override val boosters: List<Int>,
    @SerialName("available_boosters") override val availableBoosters: Map<Int, Int>,
    @SerialName("hero") override val hero: IMatchHeroInfo,
    @SerialName("avatar") override val avatar: Int? = -1, //Client cũ có thể null
) : IMatchUserInfo {
    override fun toString(): String {
        val builder = StringBuilder()
            .append("$serverId-")
            .append("$buildVersion-$matchId-$mode-")
            .append("$isTest-$isWhitelisted-$isBot-")
            .append("$userId-$username-$displayName-")
            .append("$totalMatchCount-$matchCount-$winMatchCount-$rank-$point-")
            .append("$boosters-${availableBoosters.toSortedMap()}-")
            .append("$hero")
            .append("$avatar")
        return builder.toString()
    }
}
