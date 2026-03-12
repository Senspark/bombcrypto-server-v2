package com.senspark.game.pvp.manager

class DefaultPacketManager : IPacketManager {
    private val _actions = mutableListOf<() -> Unit>()
    private val _locker = Any()

    override fun add(action: () -> Unit) {
        synchronized(_locker) {
            _actions.add(action)
        }
    }

    override fun flush() {
        val actions: List<() -> Unit>
        synchronized(_locker) {
            actions = _actions.toList()
            _actions.clear()
        }
        actions.forEach {
            it()
        }
    }
}