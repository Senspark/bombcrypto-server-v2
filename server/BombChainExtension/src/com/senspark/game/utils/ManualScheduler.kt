package com.senspark.game.utils

import com.senspark.common.service.IScheduler

class ManualScheduler : IScheduler {
    private interface IFuture {
        val key: String
        fun isFinished(): Boolean
        fun step(dt: Int)
    }

    private class Future(
        private val _action: () -> Unit,
        private var _duration: Int,
        override val key: String
    ) : IFuture {
        private var _finished = false
        override fun isFinished(): Boolean {
            return _finished
        }

        override fun step(dt: Int) {
            if (_finished) {
                throw Exception("Future is finished")
            }
            _duration -= dt
            if (_duration <= 0) {
                _finished = true
                _action()
            }
        }
    }

    private class RepeatingFuture(
        private val _action: () -> Unit,
        override val key: String,
        private var _startTime: Int,
        private val _repeatRate: Int
    ) : IFuture {
        private var _delta = 0
        override fun isFinished(): Boolean {
            return false
        }

        override fun step(dt: Int) {
            _startTime -= dt
            if (_startTime > 0) {
                return
            }
            _delta += dt
            if (_delta < _repeatRate) {
                return
            }
            _delta = 0
            _action()
        }
    }

    private val _actions = mutableMapOf<String, IFuture>()

    override fun initialize() {
    }

    override fun clearAll() {
    }

    override fun clear(key: String) {
        _actions.remove(key)
    }

    override fun scheduleOnce(key: String, delay: Int, action: () -> Unit) {
        _actions[key] = Future(action, delay, key)
    }

    override fun fireAndForget(action: () -> Unit) {
        val randomKey = "FireAndForget_${System.currentTimeMillis()}"
        scheduleOnce(randomKey, 0, action)
    }

    override fun schedule(key: String, delay: Int, interval: Int, action: () -> Unit) {
        _actions[key] = RepeatingFuture(action, key, delay, interval)
    }

    override fun isScheduled(key: String): Boolean {
        return _actions.containsKey(key)
    }

    fun step(dt: Int) {
        val remove = mutableListOf<String>()
        val actions = _actions.values
        for (it in actions) {
            it.step(dt)
            if (it.isFinished()) {
                remove.add(it.key)
            }
        }
        for (it in remove) {
            _actions.remove(it)
        }
    }
}