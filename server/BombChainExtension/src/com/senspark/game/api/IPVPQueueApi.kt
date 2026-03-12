package com.senspark.game.api

import com.senspark.common.pvp.IMatchUserInfo

interface IPvpJoinQueueInfo {
    val username: String
    val pings: Map<String, Int>
    val info: IMatchUserInfo
}