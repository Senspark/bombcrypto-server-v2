package com.senspark.game.pvp.service

import com.senspark.common.IDatabase
import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.config.PvpWagerConfig
import com.senspark.game.service.DatabaseStatement
import java.sql.ResultSet

/**
 * PvpFeeProcessor handles the batch processing of platform fees collected during PVP matches.
 * It periodically sums up pending fees from pvp_fee_ledger and transfers them to the treasury wallet.
 */
class PvpFeeProcessor(
    private val _db: IDatabase,
    private val _logger: ILogger,
    private val _statement: DatabaseStatement,
    private val _treasuryUserId: Int = 1 // Senspark Treasury Account ID
) {
    /**
     * Aggregates and processes all pending fees in the ledger.
     */
    fun processPendingFees() {
        try {
            val sqlPending = """
                SELECT token_type, network, SUM(amount) as total_amount 
                FROM pvp_fee_ledger 
                WHERE status = 'PENDING' 
                GROUP BY token_type, network
            """.trimIndent()
            
            val queryBuilder = _db.createQueryBuilder(true)
            queryBuilder.addStatement(sqlPending, emptyArray())
            
            // Execute query and process results
            queryBuilder.executeQuery().let { sfsArray ->
                for (i in 0 until sfsArray.size()) {
                    val row = sfsArray.getSFSObject(i)
                    val tokenType = row.getUtfString("token_type")
                    val network = row.getUtfString("network")
                    val totalAmount = row.getDouble("total_amount")
                    
                    if (totalAmount != null && totalAmount > 0) {
                        processBatch(tokenType, network, totalAmount)
                    }
                }
            }
        } catch (e: Exception) {
            _logger.error("[PvpFeeProcessor] Critical error in processPendingFees: ${e.message}")
        }
    }

    /**
     * Executes the transfer for a specific token/network batch and marks the ledger records as processed.
     */
    private fun processBatch(tokenType: String, network: String, amount: Double) {
        try {
            // 1. Credit the treasury wallet using the established addUserReward pattern
            // addUserReward Statement: _uid, _networktype, _amount, _rewardtype, _reason
            val updateBuilder = _db.createQueryBuilder(true)
            updateBuilder.addStatement(_statement.addUserReward, arrayOf(_treasuryUserId, network, amount, tokenType, PvpWagerConfig.TREASURY_FEE_REASON))
            val updated = updateBuilder.executeUpdate()
            
            if (updated > 0) {
                // 2. Mark all aggregated records in this batch as PROCESSED
                val markProcessed = """
                    UPDATE pvp_fee_ledger 
                    SET status = 'PROCESSED', processed_at = (NOW() AT TIME ZONE 'utc')
                    WHERE token_type = ? AND network = ? AND status = 'PENDING'
                """.trimIndent()
                
                val markBuilder = _db.createQueryBuilder(true)
                markBuilder.addStatement(markProcessed, arrayOf(tokenType, network))
                markBuilder.executeUpdate()
                
                _logger.log("[PvpFeeProcessor] Successfully processed batch fee: $amount (Token: $tokenType, Network: $network)")
            } else {
                _logger.error("[PvpFeeProcessor] Failed to credit treasury for $amount $tokenType on $network")
            }
        } catch (e: Exception) {
            _logger.error("[PvpFeeProcessor] Exception during batch processing for $tokenType/$network: ${e.message}")
        }
    }
}
