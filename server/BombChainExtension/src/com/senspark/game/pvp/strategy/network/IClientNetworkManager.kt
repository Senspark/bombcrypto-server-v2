package com.senspark.game.pvp.strategy.network

interface IClientNetworkManager {
    /** Client latency in milliseconds. */
    val latency: Int

    /** Time difference between server and client in milliseconds. */
    val timeDelta: Int

    /** Loss rate. */
    val lossRate: Float

    fun ping(
        latencies: List<Int>,
        timeDeltas: List<Int>,
        lossRates: List<Float>,
    )

    fun pong(clientTimestamp: Long, requestId: Int)
}