package com.senspark.game.utils

import com.senspark.common.service.IScheduler
import com.senspark.common.utils.ILogger

class SafeScheduler(
    private val _logger: ILogger,
    private val _scheduler: IScheduler,
) : IScheduler {

    override fun initialize() {
    }
    
    override fun clearAll() {
        _scheduler.clearAll()
    }

    override fun clear(key: String) {
        _scheduler.clear(key)
    }

    override fun scheduleOnce(key: String, delay: Int, action: () -> Unit) {
        _scheduler.scheduleOnce(key, delay) {
            try {
                action()
            } catch (ex: Exception) {
                _logger.log(ex.toString())
            }
        }
    }

    override fun fireAndForget(action: () -> Unit) {
        val randomKey = "FireAndForget_${System.currentTimeMillis()}"
        scheduleOnce(randomKey, 0, action)
    }

    override fun schedule(key: String, delay: Int, interval: Int, action: () -> Unit) {
        _scheduler.schedule(key, delay, interval) {
            try {
                action()
            } catch (ex: Exception) {
                _logger.log(ex.toString())
                _logger.log(ex.stackTraceToString())
            }
        }
    }

    override fun isScheduled(key: String): Boolean {
        return _scheduler.isScheduled(key)
    }
}