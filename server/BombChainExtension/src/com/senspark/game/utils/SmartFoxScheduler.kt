package com.senspark.game.utils

import com.senspark.common.service.IScheduler
import com.senspark.common.utils.ILogger
import com.smartfoxserver.v2.util.TaskScheduler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class SmartFoxScheduler(
    threadPoolSize: Int,
    private val _logger: ILogger,
) : IScheduler {
    private val _scheduler = TaskScheduler(threadPoolSize)
    private val _runningTasks = ConcurrentHashMap<String, ScheduledFuture<*>>()

    override fun initialize() {
    }

    override fun scheduleOnce(key: String, delay: Int, action: () -> Unit) {
        if (_runningTasks.containsKey(key)) {
            clear(key)
        }
        val task = _scheduler.schedule({
            runAction(action)
        }, delay, TimeUnit.MILLISECONDS)
        _runningTasks[key] = task
    }

    override fun fireAndForget(action: () -> Unit) {
        _scheduler.schedule({
            runAction(action)
        }, 0, TimeUnit.MILLISECONDS)
    }

    override fun schedule(key: String, delay: Int, interval: Int, action: () -> Unit) {
        if (_runningTasks.containsKey(key)) {
            clear(key)
        }
        val task = _scheduler.scheduleAtFixedRate({
            runAction(action)
        }, delay, interval, TimeUnit.MILLISECONDS)
        _runningTasks[key] = task
    }

    override fun isScheduled(key: String): Boolean {
        return _runningTasks.containsKey(key)
    }

    override fun clear(key: String) {
        _runningTasks[key]?.cancel(true)
        _runningTasks.remove(key)
    }

    override fun clearAll() {
        _runningTasks.values.forEach { it.cancel(true) }
        _runningTasks.clear()
    }

    private fun runAction(action: () -> Unit) {
        try {
            action()
        } catch (ex: Exception) {
            _logger.error(ex)
        }
    }
}