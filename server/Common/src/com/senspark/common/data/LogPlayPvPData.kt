package com.senspark.common.data

import com.senspark.common.constant.PlayPvPMatchResult
import java.time.Instant

class LogPlayPvPData(
    val betValue: Int,
    val matchId: String,
    val matchResult: PlayPvPMatchResult,
    val playDuration: Int,
    val playTime: Instant,
    val users: List<LogPlayPvPUserData>
)

