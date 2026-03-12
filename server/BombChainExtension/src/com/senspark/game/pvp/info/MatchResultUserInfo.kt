package com.senspark.game.pvp.info

import com.senspark.common.pvp.IMatchResultHeroInfo
import com.senspark.common.pvp.IMatchResultUserInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MatchResultUserInfo(
    @SerialName("server_id") override val serverId: String,
    @SerialName("is_test") override val isTest: Boolean,
    @SerialName("is_bot") override val isBot: Boolean,
    @SerialName("team_id") override val teamId: Int,
    @SerialName("user_id") override val userId: Int,
    @SerialName("username") override val username: String,
    @SerialName("match_count") override val matchCount: Int,
    @SerialName("win_match_count") override val winMatchCount: Int,
    @SerialName("point") override val point: Int,
    @SerialName("boosters") override val boosters: List<Int>,
    @SerialName("used_boosters") override val usedBoosters: Map<Int, Int>,
    @SerialName("quit") override val quit: Boolean,
    @SerialName("hero") override val hero: IMatchResultHeroInfo,
) : IMatchResultUserInfo