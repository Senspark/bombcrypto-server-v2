package com.senspark.game.service

import com.senspark.common.service.IGlobalService
import com.senspark.common.service.IService
import com.senspark.game.api.IPvpResultInfo
import com.senspark.game.pvp.config.PvpWagerTier
import com.senspark.game.pvp.config.PvpWagerToken

interface IPvpWagerService : IService, IGlobalService {
    /**
     * Debits the wager amount from the user and stores it in the pvp_wager_entry table.
     * This acts as an escrow.
     */
    fun debitEscrow(matchId: String, userId: Int, tier: PvpWagerTier, token: PvpWagerToken): Boolean

    /**
     * Locks the pool for a match and totals the entries.
     */
    fun lockPool(matchId: String): Boolean

    /**
     * Distributes the prize pool to winners based on match rankings and quit status.
     */
    fun distributePrize(resultInfo: IPvpResultInfo): Boolean

    /**
     * Refunds all wagers for a match if it fails to start or finishes abnormally.
     */
    fun refundMatch(matchId: String): Boolean
}
