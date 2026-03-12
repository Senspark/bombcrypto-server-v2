package com.senspark.game.pvp.strategy.network

interface INetworkPacket {
    val requestTimestamp: Long
    val latency: Int
    val timeDelta: Int

    /**
     * Pongs this request.
     * @param serverTimestamp Current server timestamp.
     * @param clientTimestamp When the client receives the ping request.
     */
    fun pong(serverTimestamp: Long, clientTimestamp: Long)
}