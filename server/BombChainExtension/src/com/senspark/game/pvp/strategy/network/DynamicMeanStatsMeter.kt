package com.senspark.game.pvp.strategy.network

import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.manager.ITimeManager

class DynamicMeanStatsMeter(
    private val _logger: ILogger,
    private val _timeManager: ITimeManager,
    private val _packets: Map<Int, INetworkPacket>,
    private val _pendingPacketIds: Set<Int>,
) : IStatsMeter {
    private var _sum = 0
    private var _size = 0
    override val value: Int
        get() {
            val timestamp = _timeManager.timestamp
            val size = _size + _pendingPacketIds.size
            val sum = _sum + _pendingPacketIds.sumOf {
                val packet = _packets[it] ?: return@sumOf 0
                val lastTimestamp = packet.requestTimestamp
                (timestamp - lastTimestamp).toInt()
            }
            return if (size == 0) 0 else sum / size
        }

    override fun add(value: Int) {
        _sum += value
        ++_size
    }

    override fun remove(value: Int) {
        _sum -= value
        --_size
    }
}