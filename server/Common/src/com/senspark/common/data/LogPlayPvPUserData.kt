package com.senspark.common.data

import com.senspark.common.constant.PlayPvPLoseReason

class LogPlayPvPUserData(
    val walletAddress: String,
    val heroId: Int,
    val collectedItemQuantity: Int,
    val loseReason: PlayPvPLoseReason,
    val opponentName: String? = null,
    val deltaPoint: Int = 0
)