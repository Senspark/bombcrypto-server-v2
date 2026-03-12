package com.senspark.common.service

import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor

class SimpleScheduler : IScheduler {
    private val _scheduler = ScheduledThreadPoolExecutor(1)
    private val _futureMap = mutableMapOf<String, ScheduledFuture<*>>()

    override fun initialize() {
    }

    override fun scheduleOnce(key: String, delay: Int, action: () -> Unit) {
        if (_futureMap.containsKey(key)) {
            clear(key)
        }
        _futureMap[key] = _scheduler.schedule({
            _futureMap.remove(key)
            action()
        }, delay.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
    }

    override fun fireAndForget(action: () -> Unit) {
        _scheduler.execute(action)
    }

    override fun schedule(key: String, delay: Int, interval: Int, action: () -> Unit) {
        if (_futureMap.containsKey(key)) {
            clear(key)
        }
        _futureMap[key] = _scheduler.scheduleAtFixedRate(
            action,
            delay.toLong(),
            interval.toLong(),
            java.util.concurrent.TimeUnit.MILLISECONDS
        )
    }

    override fun isScheduled(key: String): Boolean {
        return _futureMap.containsKey(key)
    }

    override fun clear(key: String) {
        _futureMap[key]?.cancel(true)
        _futureMap.remove(key)
    }

    override fun clearAll() {
        _futureMap.values.forEach { it.cancel(true) }
        _futureMap.clear()
    }
}