package com.senspark.game.pvp.manager

import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.data.IMatch
import com.senspark.game.pvp.delta.IMatchStateDelta
import com.senspark.game.pvp.strategy.state.DefaultMatchStateComparer

class DefaultStateManager(
    private val _logger: ILogger,
    private val _match: IMatch,
) : IStateManager {
    private val _matchComparer = DefaultMatchStateComparer(_logger)

    /** Initial match state. */
    private val _initialState = _match.state

    /** Last match state. */
    private var _matchState = _initialState

    override val accumulativeChangeData: IMatchStateDelta?
        get() {
            val state = _match.state
            return _matchComparer.compare(state, _initialState)
        }

    override fun processState(): IMatchStateDelta? {
        // Current states.
        val state = _match.state

        // Compare states.
        val delta = _matchComparer.compare(state, _matchState) ?: return null

        // Update last state.
        _matchState = state

        // Return result.
        return delta
    }
}