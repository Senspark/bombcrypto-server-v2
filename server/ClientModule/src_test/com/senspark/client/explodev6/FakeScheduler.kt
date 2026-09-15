package com.senspark.client.explodev6

import com.senspark.common.service.IScheduler

// Records scheduled actions instead of timing them; tests fire fuses via [fire] / [fireAll].
class FakeScheduler : IScheduler {
    private val _scheduled = mutableMapOf<String, () -> Unit>()

    val pendingCount: Int get() = _scheduled.size

    override fun initialize() {}

    override fun scheduleOnce(key: String, delay: Int, action: () -> Unit) {
        _scheduled[key] = action
    }

    override fun fireAndForget(action: () -> Unit) {
        action()
    }

    override fun schedule(key: String, delay: Int, interval: Int, action: () -> Unit) {
        _scheduled[key] = action
    }

    override fun isScheduled(key: String): Boolean = _scheduled.containsKey(key)

    override fun clear(key: String) {
        _scheduled.remove(key)
    }

    override fun clearAll() {
        _scheduled.clear()
    }

    fun fireAll() {
        val toRun = _scheduled.values.toList()
        _scheduled.clear()
        toRun.forEach { it() }
    }

    // Returns whether an action was armed under [key].
    fun fire(key: String): Boolean {
        val action = _scheduled.remove(key) ?: return false
        action()
        return true
    }
}
