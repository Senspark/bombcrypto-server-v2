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
    override val game_mode: Int = 1,
    override val wager_mode: Int = 0,
    override val wager_tier: Int = 0,
    override val wager_token: Int = 0,
) : IMatchRuleInfoClient {
    override fun toString(): String {
        val builder = StringBuilder()
            .append("$room_size-$team_size-$can_draw-$round-$is_tournament-$game_mode-$wager_mode-$wager_tier-$wager_token")
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
    override val gameMode: Int = 1,
    override val wagerMode: Int = 0,
    override val wagerTier: Int = 0,
    override val wagerToken: Int = 0,
) : IMatchRuleInfo {
    override fun toString(): String {
        val builder = StringBuilder()
            .append("$roomSize-$teamSize-$canDraw-$round-$isTournament-$gameMode-$wagerMode-$wagerTier-$wagerToken")
        return builder.toString()
    }
}