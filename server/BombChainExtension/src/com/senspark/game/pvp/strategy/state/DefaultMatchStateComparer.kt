package com.senspark.game.pvp.strategy.state

import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.data.IMatchState
import com.senspark.game.pvp.delta.IMatchStateDelta
import com.senspark.game.pvp.delta.MatchStateDelta

class DefaultMatchStateComparer(
    private val _logger: ILogger,
) : IMatchStateComparer {
    private val _heroComparer = DefaultHeroStateComparer()
    private val _bombComparer = DefaultBombStateComparer(_logger)
    private val _mapComparer = DefaultMapStateComparer()
    
    override fun compare(
        state: IMatchState,
        lastState: IMatchState,
    ): IMatchStateDelta? {
        val heroDelta = _heroComparer.compare(state.heroState, lastState.heroState)
        val bombDelta = _bombComparer.compare(state.bombState, lastState.bombState)
        val blockDelta = _mapComparer.compare(state.mapState, lastState.mapState)
        if (heroDelta.isEmpty() &&
            bombDelta.isEmpty() &&
            blockDelta.isEmpty()) {
            return null
        }
        return MatchStateDelta(
            hero = heroDelta,
            bomb = bombDelta,
            block = blockDelta,
        )
    }
}