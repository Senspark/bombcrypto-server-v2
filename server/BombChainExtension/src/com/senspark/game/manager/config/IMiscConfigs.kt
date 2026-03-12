package com.senspark.game.manager.config

interface IMiscConfigs {
    val pvpMatchCount: Int
    val lastReceiveOfflineReward: Long
    fun inCreatePvpMatchCount(isWin: Boolean)
}