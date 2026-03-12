package com.senspark.game.pvp

interface IQueueUser {
    val isWhitelisted: Boolean
    val point: IUserPoint
    val bet: Int
    var isInMatch: Boolean
    fun isMatch(other: IQueueUser, delta: Pair<Int, Int>): Boolean
}