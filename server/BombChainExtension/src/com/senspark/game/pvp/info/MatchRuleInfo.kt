package com.senspark.game.pvp.info

import com.senspark.common.pvp.IMatchRuleInfo
import com.senspark.common.pvp.IMatchRuleInfoClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MatchRuleInfoClient(
    override val room_size: Int,
    override val team_size: Int,
    override val can_draw: Boolean,
    override val round: Int,
    override val is_tournament: Boolean,
) : IMatchRuleInfoClient {
    override fun toString(): String {
        val builder = StringBuilder()
            .append("$room_size-$team_size-$can_draw-$round-$is_tournament")
        return builder.toString()
    }
}

@Serializable
class MatchRuleInfo(
    override val roomSize: Int,
    override val teamSize: Int,
    override val canDraw: Boolean,
    override val round: Int,
    override val isTournament: Boolean,
) : IMatchRuleInfo {
    override fun toString(): String {
        val builder = StringBuilder()
            .append("$roomSize-$teamSize-$canDraw-$round-$isTournament")
        return builder.toString()
    }
}