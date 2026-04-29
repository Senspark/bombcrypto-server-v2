package com.senspark.game.pvp.config

object PvpWagerConfig {
    const val FEE_PERCENTAGE = 0.05 // 5%
    const val TREASURY_FEE_REASON = "PVP Match Fee"
    const val ESCROW_REASON = "PVP Wager Escrow"
    const val PRIZE_REASON = "PVP Match Prize"
    const val REFUND_REASON = "PVP Match Refund"

    // Distribuicao Battle Royale 6P
    val BR_PRIZE_SPLIT = mapOf(
        1 to 0.70, // 1st
        2 to 0.20, // 2nd
        3 to 0.10  // 3rd
    )

    // Match Timeout for automatic refund (in seconds)
    const val MATCH_START_TIMEOUT = 60
}
