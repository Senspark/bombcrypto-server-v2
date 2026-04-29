package com.senspark.game.manager.pvp

import com.senspark.game.controller.IUserController
import com.senspark.game.pvp.config.PvpWagerTier
import com.senspark.game.pvp.config.PvpWagerToken
import com.senspark.common.utils.ILogger
import com.senspark.common.pvp.PvpMode

object PvpWagerManager {

    fun processWagerJoin(
        userController: IUserController,
        token: PvpWagerToken,
        tier: PvpWagerTier,
        logger: ILogger
    ): Boolean {
        if (token == PvpWagerToken.NONE || tier == PvpWagerTier.NONE) {
            return true
        }

        logger.log("[PvpWager] Deducting wager for ${userController.userName}: ${tier.amount} ${token.displayName}")

        try {
            userController.masterUserManager.blockRewardManager.deductReward(
                tier.amount.toFloat(),
                token.rewardType,
                token.network
            )
            return true
        } catch (e: Exception) {
            logger.log("[PvpWager] Failed to deduct wager for ${userController.userName}: ${e.message}")
            return false
        }
    }

    /**
     * Logic for calculating prize distribution based on rankings.
     * Battle Royale: 1st (70%), 2nd (20%), 3rd (10%) after fee.
     * Teams: Winning team splits the pot.
     */
    fun calculatePrizeDistribution(
        playerCount: Int,
        wagerAmount: Float,
        isBattleRoyale: Boolean,
        feePercent: Float = 0.05f
    ): Map<Int, Float> {
        val totalPot = playerCount * wagerAmount
        val potAfterFee = (totalPot * (1.0 - feePercent)).toFloat()
        
        val distribution = mutableMapOf<Int, Float>()

        if (isBattleRoyale) {
            // Battle Royale (FFA_6) distribution: 1st (70%), 2nd (20%), 3rd (10%)
            distribution[1] = potAfterFee * 0.70f
            distribution[2] = potAfterFee * 0.20f
            distribution[3] = potAfterFee * 0.10f
        } else {
            // Default (1v1, 2v2, 3v3): Winner takes all
            distribution[1] = potAfterFee
        }
        
        return distribution
    }
}
