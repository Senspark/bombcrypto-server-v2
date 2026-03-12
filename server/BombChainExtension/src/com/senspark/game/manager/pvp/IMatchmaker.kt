package com.senspark.game.manager.pvp

import com.senspark.common.pvp.IMatchInfo
import com.senspark.game.api.IPvpJoinQueueInfo

interface IMatchmakerListener {
    /**
     * Occurs when a match is found.
     * @param username The desired user.
     * @param info Match info.
     */
    fun onMatchFound(
        username: String,
        info: IMatchInfo,
    )
}

interface IMatchmaker {
    fun join(info: IPvpJoinQueueInfo): Boolean
    fun keepJoining(username: String)
    fun leave(username: String): Boolean
}