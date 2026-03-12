package com.senspark.game.utils

import com.senspark.common.service.IScheduler
import com.senspark.common.utils.ILogger
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class DefaultScheduler(
    private val _logger: ILogger,
    private val _executor: ScheduledThreadPoolExecutor,
) : IScheduler {
    private val _futureMap = mutableMapOf<String, ScheduledFuture<*>>()

    override fun initialize() {
    }

    override fun clearAll() {
        _futureMap.values.forEach { it.cancel(true) }
        _futureMap.clear()
    }

    override fun clear(key: String) {
        _futureMap[key]?.cancel(true)
        _futureMap.remove(key)
    }

    override fun scheduleOnce(key: String, delay: Int, action: () -> Unit) {
        if (_futureMap.containsKey(key)) {
            clear(key)
        }
        _futureMap[key] = _executor.schedule({
            _futureMap.remove(key)
            action()
        }, delay.toLong(), TimeUnit.MILLISECONDS)
    }

    override fun fireAndForget(action: () -> Unit) {
        val randomKey = "FireAndForget_${System.currentTimeMillis()}"
        scheduleOnce(randomKey, 0, action)
    }

    override fun schedule(key: String, delay: Int, interval: Int, action: () -> Unit) {
        if (_futureMap.containsKey(key)) {
            clear(key)
        }
        _futureMap[key] = _executor.scheduleAtFixedRate(
            action,
            delay.toLong(),
            interval.toLong(),
            TimeUnit.MILLISECONDS
        )
    }

    override fun isScheduled(key: String): Boolean {
        return _futureMap.containsKey(key)
    }
}