package com.senspark.game.pvp.strategy.network

import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.manager.ITimeManager

class DynamicMedianStatsMeter(
    private val _logger: ILogger,
    private val _timeManager: ITimeManager,
    private val _packets: Map<Int, INetworkPacket>,
    private val _pendingPacketIds: Set<Int>,
) : IStatsMeter {
    private var _dirty = false
    private val _values = mutableListOf<Int>()

    override val value: Int
        get() {
            val timestamp = _timeManager.timestamp
            val items = _pendingPacketIds.map {
                val packet = _packets[it] ?: return@map 0
                val lastTimestamp = packet.requestTimestamp
                (timestamp - lastTimestamp).toInt()
            }.sorted()
            if (_dirty) {
                _dirty = false
                _values.sort()
            }
            val allItems = (items + _values).sorted()
            return if (allItems.isEmpty()) 0 else allItems[allItems.size / 2]
        }

    override fun add(value: Int) {
        _values.add(value)
        _dirty = true
    }

    override fun remove(value: Int) {
        _values.remove(value)
        _dirty = true
    }
}