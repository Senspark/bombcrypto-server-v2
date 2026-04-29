package com.senspark.game.pvp.security

import com.senspark.common.pvp.IMatchController
import com.senspark.common.pvp.MatchStatus
import com.senspark.common.utils.ILogger
import com.smartfoxserver.v2.entities.User

/**
 * Handles disconnection penalties and refund conditions to prevent match manipulation.
 * Implements the "Disconnect = Forfeit" policy for wagered matches.
 */
class PvpMatchShield(
    private val _controller: IMatchController,
    private val _logger: ILogger
) {
    /**
     * Handles player disconnection. 
     * If the match is already in progress or tokens are locked, the player is forced to quit (forfeit).
     */
    fun handleDisconnect(user: User) {
        val matchData = _controller.matchData
        val status = matchData.status
        _logger.log("[PvpMatchShield] Player disconnected: user=${user.name} match=${matchData.id} status=$status")
        
        // Forfeit if match is active or ready (tokens are already locked)
        // This prevents players from disconnecting to avoid a loss or to manipulate results.
        if (status == MatchStatus.Started || status == MatchStatus.Ready || status == MatchStatus.Finished) {
            _logger.log("[PvpMatchShield] Match in progress or finishing. Marking as FORFEIT for user=${user.name}")
            _controller.quit(user)
        }
    }
    
    /**
     * Determines if a refund is eligible for a match that didn't fully start.
     * Refunds are ONLY allowed if the match stayed in the initial state.
     */
    fun isRefundEligible(): Boolean {
        val status = _controller.matchData.status
        return status == MatchStatus.MatchStarted
    }
}
