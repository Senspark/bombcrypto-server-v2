package com.senspark.game.pvp.manager

import com.senspark.game.pvp.user.IUserController

interface INetworkManager : IUpdater {
    val latencies: List<Int>
    val timeDeltas: List<Int>
    val lossRates: List<Float>
    fun pong(controller: IUserController, timestamp: Long, requestId: Int)
}