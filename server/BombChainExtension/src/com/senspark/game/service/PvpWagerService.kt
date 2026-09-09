package com.senspark.game.service

import com.senspark.common.IDatabase
import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.config.PvpWagerConfig
import com.senspark.game.pvp.config.PvpWagerTier
import com.senspark.game.pvp.config.PvpWagerToken
import com.senspark.game.api.IPvpResultInfo
import com.smartfoxserver.v2.entities.data.SFSArray
import java.sql.ResultSet

class PvpWagerService(
    private val _db: IDatabase,
    private val _logger: ILogger,
    private val _statement: DatabaseStatement
) : IPvpWagerService {

    override fun initialize() {
        _logger.log("PvpWagerService initialized")
    }

    override fun destroy() {}

    private fun String.executeQuery(vararg params: Any) {
        try {
            _db.createQueryBuilder(true).addStatement(this, arrayOf(*params)).executeQuery()
        } catch (ex: Exception) {
            _logger.error("DB Error in PvpWagerService: $this", ex)
        }
    }

    private fun String.executeUpdate(vararg params: Any) {
        try {
            _db.createQueryBuilder(true).addStatement(this, arrayOf(*params)).executeUpdate()
        } catch (ex: Exception) {
            _logger.error("DB Error in PvpWagerService: $this", ex)
        }
    }

    override fun debitEscrow(matchId: String, userId: Int, tier: PvpWagerTier, token: PvpWagerToken): Boolean {
        if (tier == PvpWagerTier.NONE || token == PvpWagerToken.NONE) return true

        val amount = tier.amount
        val network = token.network.name
        val rewardType = token.rewardType.name

        return try {
            // Security First: Use a single transaction for balance deduction and entry logging
            val builder = _db.createQueryBuilder(true)
            
            // 1. Check balance and debit using fn_sub_user_reward
            builder.addStatement(_statement.subUserReward, arrayOf(userId, network, amount, rewardType, PvpWagerConfig.ESCROW_REASON))

            // 2. Insert into pvp_wager_entry
            val sqlEntry = """
                INSERT INTO pvp_wager_entry (match_id, user_id, amount, token_type, network, status)
                VALUES (?, ?, ?, ?, ?, 'PENDING')
                ON CONFLICT (match_id, user_id) DO UPDATE SET status = 'PENDING';
            """.trimIndent()
            builder.addStatementUpdate(sqlEntry, arrayOf(matchId, userId, amount, rewardType, network))

            // 3. Upsert pool
            val sqlPool = """
                INSERT INTO pvp_wager_pool (match_id, token_type, network, total_pool, fee_amount, status)
                VALUES (?, ?, ?, 0, 0, 'OPEN')
                ON CONFLICT (match_id) DO NOTHING;
            """.trimIndent()
            builder.addStatementUpdate(sqlPool, arrayOf(matchId, rewardType, network))

            builder.executeMultiQuery()
            true
        } catch (e: Exception) {
            _logger.error("[Security] Failed to debit escrow for user $userId in match $matchId", e)
            false
        }
    }

    override fun lockPool(matchId: String): Boolean {
        return try {
            val sql = """
                UPDATE pvp_wager_pool
                SET total_pool = (SELECT SUM(amount) FROM pvp_wager_entry WHERE match_id = ?),
                    fee_amount = (SELECT SUM(amount) * ? FROM pvp_wager_entry WHERE match_id = ?),
                    status = 'LOCKED',
                    updated_at = (NOW() AT TIME ZONE 'utc')
                WHERE match_id = ?;
            """.trimIndent()
            sql.executeUpdate(matchId, PvpWagerConfig.FEE_PERCENTAGE, matchId, matchId)

            val sqlEntries = "UPDATE pvp_wager_entry SET status = 'COMMITTED' WHERE match_id = ?;"
            sqlEntries.executeUpdate(matchId)

            true
        } catch (e: Exception) {
            _logger.error("Failed to lock pool for match $matchId", e)
            false
        }
    }

    override fun distributePrize(resultInfo: IPvpResultInfo): Boolean {
        val matchId = resultInfo.id
        return try {
            var tokenType = ""
            var network = ""
            var totalPool = 0.0
            var feeAmount = 0.0

            // Security First: Select FOR UPDATE to lock the pool record
            val sqlGetPool = "SELECT token_type, network, total_pool, fee_amount FROM pvp_wager_pool WHERE match_id = ? FOR UPDATE"
            _db.createQueryBuilder(true).addStatement(sqlGetPool, arrayOf(matchId)).executeQuery { rs ->
                if (rs.next()) {
                    tokenType = rs.getString("token_type")
                    network = rs.getString("network")
                    totalPool = rs.getDouble("total_pool")
                    feeAmount = rs.getDouble("fee_amount")
                }
            }

            if (totalPool <= 0) return false

            val netPool = totalPool - feeAmount

            // FASE 3: Blindagem - Mark quitted users as FORFEIT
            resultInfo.info.forEach { user ->
                if (user.quit) {
                    val sqlForfeit = "UPDATE pvp_wager_entry SET status = 'FORFEIT' WHERE match_id = ? AND user_id = ?"
                    sqlForfeit.executeUpdate(matchId, user.userId)
                    _logger.log("[Pvp][Wager] User ${user.userId} marked as FORFEIT in match $matchId")
                }
            }

            // Distribute based on mode
            if (resultInfo.mode == com.senspark.common.pvp.PvpMode.BATTLE_ROYALE) {
                // Battle Royale: 1st (70%), 2nd (20%), 3rd (10%)
                // Quitted users get 0 even if their rank is 1, 2 or 3.
                val builder = _db.createQueryBuilder(true)
                resultInfo.info.filter { !it.quit && it.ranking <= 3 }.forEach { user ->
                    val split = PvpWagerConfig.BR_PRIZE_SPLIT[user.ranking] ?: 0.0
                    if (split > 0) {
                        val prize = netPool * split
                        builder.addStatementUpdate(_statement.addUserReward, arrayOf(user.userId, network, prize, tokenType, PvpWagerConfig.PRIZE_REASON))
                        
                        val sqlWin = "UPDATE pvp_wager_entry SET status = 'WON', amount = ? WHERE match_id = ? AND user_id = ?"
                        builder.addStatementUpdate(sqlWin, arrayOf(prize, matchId, user.userId))
                    }
                }
                builder.executeMultiQuery()
            } else {
                // Team Mode (1v1, 2v2, 3v3)
                // winningTeam members divide the netPool equally, BUT quitted members get 0.
                if (resultInfo.winningTeam != -1 && !resultInfo.isDraw) {
                    val winningMembers = resultInfo.info.filter { it.teamId == resultInfo.winningTeam && !it.quit }
                    if (winningMembers.isNotEmpty()) {
                        val prizePerMember = netPool / winningMembers.size
                        val builder = _db.createQueryBuilder(true)
                        winningMembers.forEach { user ->
                            builder.addStatementUpdate(_statement.addUserReward, arrayOf(user.userId, network, prizePerMember, tokenType, PvpWagerConfig.PRIZE_REASON))
                            
                            val sqlWin = "UPDATE pvp_wager_entry SET status = 'WON', amount = ? WHERE match_id = ? AND user_id = ?"
                            builder.addStatementUpdate(sqlWin, arrayOf(prizePerMember, matchId, user.userId))
                        }
                        builder.executeMultiQuery()
                    }
                }
            }

            // Log fee
            val sqlFee = "INSERT INTO pvp_fee_ledger (match_id, amount, token_type, network, status) VALUES (?, ?, ?, ?, 'PENDING')"
            sqlFee.executeUpdate(matchId, feeAmount, tokenType, network)

            val sqlPaid = "UPDATE pvp_wager_pool SET status = 'PAID', updated_at = (NOW() AT TIME ZONE 'utc') WHERE match_id = ?"
            sqlPaid.executeUpdate(matchId)

            true
        } catch (e: Exception) {
            _logger.error("Failed to distribute prize for match $matchId", e)
            false
        }
    }

    override fun refundMatch(matchId: String): Boolean {
        return try {
            val builder = _db.createQueryBuilder(true)
            val sqlGetEntries = "SELECT user_id, amount, token_type, network FROM pvp_wager_entry WHERE match_id = ? AND status <> 'REFUNDED'"
            _db.createQueryBuilder(true).addStatement(sqlGetEntries, arrayOf(matchId)).executeQuery { rs ->
                while (rs.next()) {
                    val userId = rs.getInt("user_id")
                    val amount = rs.getDouble("amount")
                    val token = rs.getString("token_type")
                    val net = rs.getString("network")
                    
                    builder.addStatementUpdate(_statement.addUserReward, arrayOf(userId, net, amount, token, PvpWagerConfig.REFUND_REASON))
                }
            }

            val sqlUpdateEntries = "UPDATE pvp_wager_entry SET status = 'REFUNDED' WHERE match_id = ?"
            builder.addStatementUpdate(sqlUpdateEntries, arrayOf(matchId))

            val sqlUpdatePool = "UPDATE pvp_wager_pool SET status = 'REFUNDED', updated_at = (NOW() AT TIME ZONE 'utc') WHERE match_id = ?"
            builder.addStatementUpdate(sqlUpdatePool, arrayOf(matchId))

            builder.executeMultiQuery()
            true
        } catch (e: Exception) {
            _logger.error("[Security] Failed to refund match $matchId", e)
            false
        }
    }
}
