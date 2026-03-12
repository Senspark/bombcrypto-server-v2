package com.senspark.game.pvp.manager

import com.senspark.common.utils.ILogger

class DefaultUpdater(
    private val _logger: ILogger,
    private val _items: List<IUpdater>,
) : IUpdater {
    override fun step(delta: Int) {
        _items.forEach {
            it.step(delta)
        }
    }
}