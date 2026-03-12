package com.senspark.game.pvp.strategy.network

class NetworkPacket(
    override val requestTimestamp: Long,
) : INetworkPacket {
    private var _responseTimestamp = 0L
    private var _clientTimestamp = 0L

    override val latency get() = (_responseTimestamp - requestTimestamp).toInt()
    override val timeDelta get() = (_responseTimestamp - _clientTimestamp - latency / 2).toInt()

    override fun pong(serverTimestamp: Long, clientTimestamp: Long) {
        _responseTimestamp = serverTimestamp
        _clientTimestamp = clientTimestamp
    }
}