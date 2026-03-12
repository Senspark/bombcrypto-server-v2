package com.senspark.game.pvp.manager

class StepTimeManager : ITimeManager {
    private var _timestamp = 0L
    override val timestamp get() = _timestamp

    fun step(delta: Int) {
        _timestamp += delta
    }
}