package com.senspark.game.pvp.strategy.network

import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.data.PingPongData
import com.senspark.game.pvp.manager.IMessageBridge
import com.senspark.game.pvp.manager.ITimeManager
import com.senspark.game.pvp.user.IUserController

private enum class PacketStatus {
    Pending,
    Failed, // Time-out.
    Successful,
}

class ClientNetworkManager(
    private val _controller: IUserController,
    private val _logger: ILogger,
    private val _messageBridge: IMessageBridge,
    private val _timeManager: ITimeManager,
    private val _isParticipant: Boolean,
    private val _maxQueueSize: Int,
    private val _timeOutSize: Int,
) : IClientNetworkManager {
    private class PacketData(
        val packet: INetworkPacket,
        var status: PacketStatus
    )

    private val _locker = Any()

    private val _packets = mutableMapOf<Int, PacketData>()
    private val _latencyMeter: IStatsMeter = MeanStatsMeter()
    private val _timeDeltaMeter: IStatsMeter = MedianStatsMeter()

    private var _requestId = 0
    private var _pendingPackets = 0
    private var _successfulPackets = 0
    private var _failedPackets = 0

    override val latency: Int
        get() {
            synchronized(_locker) {
                return _latencyMeter.value
            }
        }
    override val timeDelta: Int
        get() {
            synchronized(_locker) {
                return _timeDeltaMeter.value
            }
        }

    override val lossRate: Float
        get() {
            synchronized(_locker) {
                val totalPackets = _pendingPackets + _failedPackets + _successfulPackets
                return if (totalPackets == 0) 1f else _failedPackets.toFloat() / totalPackets
            }
        }

    override fun ping(
        latencies: List<Int>,
        timeDeltas: List<Int>,
        lossRates: List<Float>,
    ) {
        // Add owned stats for observers.
        val auxLatencies = if (_isParticipant) emptyList() else listOf(latency)
        val auxTimeDeltas = if (_isParticipant) emptyList() else listOf(timeDelta)
        val auxLossRates = if (_isParticipant) emptyList() else listOf(lossRate)

        // Create a new packet.
        val timestamp = _timeManager.timestamp
        val requestId: Int
        synchronized(_locker) {
            requestId = _requestId++;
            ++_pendingPackets
            _packets[requestId] = PacketData(
                packet = NetworkPacket(timestamp),
                status = PacketStatus.Pending,
            )
            // Handle time-out packet.
            val timeOutRequestId = requestId - _timeOutSize
            _packets[timeOutRequestId]?.let {
                when (it.status) {
                    PacketStatus.Pending -> {
                        // Not responded yet => time-out.
                        --_pendingPackets
                        ++_failedPackets
                        it.status = PacketStatus.Failed
                    }

                    PacketStatus.Failed -> {
                        require(false) { "Invalid status" }
                    }

                    PacketStatus.Successful -> {
                        // Responded.
                    }
                }
            }
            // Remove old packet.
            val expiredRequestId = requestId - _maxQueueSize
            _packets.remove(expiredRequestId)?.let {
                when (it.status) {
                    PacketStatus.Pending -> {
                        require(false) { "Invalid status" }
                    }

                    PacketStatus.Failed -> {
                        --_failedPackets
                    }

                    PacketStatus.Successful -> {
                        --_successfulPackets
                        _latencyMeter.remove(it.packet.latency)
                        _timeDeltaMeter.remove(it.packet.timeDelta)
                    }
                }
            }
        }

        // Retrieve the current associated user.
        val user = _controller.user ?: return

        // Send ping request.
        val data = PingPongData(
            requestId,
            latencies + auxLatencies,
            timeDeltas + auxTimeDeltas,
            lossRates + auxLossRates,
        )
        _messageBridge.ping(data, listOf(user))
    }

    override fun pong(clientTimestamp: Long, requestId: Int) {
        synchronized(_locker) {
            val serverTimestamp = _timeManager.timestamp
            val packet = _packets[requestId] ?: return@synchronized // Expired request.
            when (packet.status) {
                PacketStatus.Pending -> {
                    --_pendingPackets
                    ++_successfulPackets
                    packet.packet.pong(serverTimestamp, clientTimestamp)
                    packet.status = PacketStatus.Successful
                    _latencyMeter.add(packet.packet.latency)
                    _timeDeltaMeter.add(packet.packet.timeDelta)
                }

                PacketStatus.Failed -> {
                    // Time-out.
                }

                PacketStatus.Successful -> {
                    require(false) { "Invalid status" }
                }
            }
        }
    }
}