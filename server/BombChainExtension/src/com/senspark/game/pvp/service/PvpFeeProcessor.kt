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
            // Use a cutoff time to prevent processing records that arrive during this execution
            val sqlPending = """
                SELECT token_type, network, SUM(amount) as total_amount, MAX(created_at) as cutoff
                FROM pvp_fee_ledger 
                WHERE status = 'PENDING' 
                GROUP BY token_type, network
            """.trimIndent()
            
            val queryBuilder = _db.createQueryBuilder(true)
            queryBuilder.addStatement(sqlPending, emptyArray())
            
            queryBuilder.executeQuery().let { sfsArray ->
                for (i in 0 until sfsArray.size()) {
                    val row = sfsArray.getSFSObject(i)
                    val tokenType = row.getUtfString("token_type")
                    val network = row.getUtfString("network")
                    val totalAmount = row.getDouble("total_amount")
                    val cutoff = row.getUtfString("cutoff") // SFSObject converts timestamp to string
                    
                    if (totalAmount != null && totalAmount > 0) {
                        processBatch(tokenType, network, totalAmount, cutoff)
                    }
                }
            }
        } catch (e: Exception) {
            _logger.error("[PvpFeeProcessor] Critical error in processPendingFees: ${e.message}")
        }
    }

    private fun processBatch(tokenType: String, network: String, amount: Double, cutoff: String) {
        try {
            val updateBuilder = _db.createQueryBuilder(true)
            updateBuilder.addStatement(_statement.addUserReward, arrayOf(_treasuryUserId, network, amount, tokenType, PvpWagerConfig.TREASURY_FEE_REASON))
            val updated = updateBuilder.executeUpdate()
            
            if (updated > 0) {
                // Mark ONLY the records that were included in this specific batch
                val markProcessed = """
                    UPDATE pvp_fee_ledger 
                    SET status = 'TRANSFERRED', processed_at = (NOW() AT TIME ZONE 'utc')
                    WHERE token_type = ? AND network = ? AND status = 'PENDING' AND created_at <= ?::timestamp
                """.trimIndent()
                
                val markBuilder = _db.createQueryBuilder(true)
                markBuilder.addStatement(markProcessed, arrayOf(tokenType, network, cutoff))
                markBuilder.executeUpdate()
                
                _logger.log("[PvpFeeProcessor] Successfully processed batch fee: $amount (Token: $tokenType, Network: $network, Cutoff: $cutoff)")
            }
 else {
                _logger.error("[PvpFeeProcessor] Failed to credit treasury for $amount $tokenType on $network")
            }
        } catch (e: Exception) {
            _logger.error("[PvpFeeProcessor] Exception during batch processing for $tokenType/$network: ${e.message}")
        }
    }
}
