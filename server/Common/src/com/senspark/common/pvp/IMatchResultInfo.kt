package com.senspark.common.pvp

interface IMatchResultInfo {
    val isDraw: Boolean
    val winningTeam: Int
    val scores: List<Int>

    val duration: Int
    val startTimestamp: Long
    
    val info: List<IMatchResultUserInfo>
}