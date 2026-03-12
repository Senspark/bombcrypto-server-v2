package com.senspark.game.pvp.manager

import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.strategy.network.ClientNetworkManager
import com.senspark.game.pvp.strategy.network.IClientNetworkManager
import com.senspark.game.pvp.user.IUserController

class DefaultNetworkManager(
    participantControllers: List<IUserController>,
    observerControllers: List<IUserController>,
    private val _logger: ILogger,
    messageBridge: IMessageBridge,
    private val _timeManager: ITimeManager,
    /** Interval between two consecutive pings in milliseconds. */
    private val _interval: Int,
    /** Max packet queue size. */
    maxQueueSize: Int,
    timeOutSize: Int,
) : INetworkManager {
    private val _participantManagers: List<IClientNetworkManager> = participantControllers.map {
        ClientNetworkManager(it, _logger, messageBridge, _timeManager, true, maxQueueSize, timeOutSize)
    }
    private val _observerManagers: List<IClientNetworkManager> = observerControllers.map {
        ClientNetworkManager(it, _logger, messageBridge, _timeManager, false, maxQueueSize, timeOutSize)
    }

    private val _controllers = participantControllers + observerControllers
    private val _managers = _participantManagers + _observerManagers

    private var _elapsed = 0f

    override val latencies get() = _managers.map { it.latency }
    override val timeDeltas get() = _managers.map { it.timeDelta }
    override val lossRates get() = _managers.map { it.lossRate }

    private fun ping() {
        val latencies = _participantManagers.map { it.latency }
        val timeDeltas = _participantManagers.map { it.timeDelta }
        val lossRates = _participantManagers.map { it.lossRate }
        _managers.forEach {
            it.ping(latencies, timeDeltas, lossRates)
        }
    }

    override fun pong(controller: IUserController, timestamp: Long, requestId: Int) {
        val index = _controllers.indexOf(controller)
        if (index < 0) {
            return
        }
        val manager = _managers[index]
        manager.pong(timestamp, requestId)
    }

    override fun step(delta: Int) {
        _elapsed += delta
        if (_elapsed < _interval) {
            return
        }
        _elapsed -= _interval
        ping()
    }
}