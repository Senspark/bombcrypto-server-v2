package com.senspark.game.pvp.info

import com.senspark.common.pvp.IMatchTeamInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MatchTeamInfo(
    override val slots: List<Int>,
) : IMatchTeamInfo {
    override fun toString(): String {
        val builder = StringBuilder()
            .append("$slots-")
        return builder.toString()
    }
}