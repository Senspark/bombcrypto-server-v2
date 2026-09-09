package com.senspark.game.service

import com.senspark.common.IDatabase
import com.senspark.common.utils.ILogger
import com.senspark.game.api.IPvpResultInfo
import com.senspark.game.api.IPvpResultUserInfo
import com.senspark.game.pvp.config.PvpWagerTier
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.util.*

interface IPvpRankingService {
    fun updateRankings(resultInfo: IPvpResultInfo)
}

class PvpRankingService(
    private val _db: IDatabase,
    private val _logger: ILogger
) : IPvpRankingService {

    override fun updateRankings(resultInfo: IPvpResultInfo) {
        val now = LocalDate.now()
        val week = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val year = now.get(IsoFields.WEEK_BASED_YEAR)
        val month = now.monthValue
        val monthYear = now.year
        val gameMode = resultInfo.mode.name

        resultInfo.info.forEachIndexed { index, userInfo ->
            if (userInfo.isBot) return@forEachIndexed

            val isWinner = resultInfo.winningTeam == userInfo.teamId && !resultInfo.isDraw
            val isLoser = resultInfo.winningTeam != userInfo.teamId && !resultInfo.isDraw
            
            // Calculate Points (Simplified ELO/Static for now as per industry best practices)
            // +25 for win, -20 for loss, +5 for draw
            val deltaPoint = when {
                isWinner -> 25
                isLoser -> -20
                else -> 5
            }

            val wagerAmount = if (resultInfo.wagerMode == 1) {
                PvpWagerTier.from(resultInfo.wagerTier).amount.toDouble()
            } else 0.0

            val prizeWon = if (isWinner && resultInfo.wagerMode == 1) {
                // Simplified: assuming winner takes pool - fee. 
                // Actual prize distribution is handled by PvpWagerService, 
                // but we track total_won here for ranking metadata.
                val pool = resultInfo.info.size * wagerAmount
                pool * 0.95 // 5% fee
            } else 0.0

            updateWeeklyRanking(userInfo.userId, week, year, gameMode, deltaPoint, isWinner, isLoser)
            updateMonthlyRanking(userInfo.userId, month, monthYear, gameMode, deltaPoint, isWinner, isLoser, wagerAmount, prizeWon)
        }
    }

    private fun updateWeeklyRanking(userId: Int, week: Int, year: Int, gameMode: String, deltaPoint: Int, isWinner: Boolean, isLoser: Boolean) {
        val sql = """
            INSERT INTO pvp_weekly_ranking (user_id, week_number, year, game_mode, points, wins, losses, matches_played, tier)
            VALUES (?, ?, ?, ?, ?, ?, ?, 1, (SELECT tier FROM pvp_tier_config WHERE min_points <= ? ORDER BY min_points DESC LIMIT 1))
            ON CONFLICT (user_id, week_number, year, game_mode) DO UPDATE SET
                points = GREATEST(0, pvp_weekly_ranking.points + excluded.points),
                wins = pvp_weekly_ranking.wins + excluded.wins,
                losses = pvp_weekly_ranking.losses + excluded.losses,
                matches_played = pvp_weekly_ranking.matches_played + 1,
                tier = (SELECT tier FROM pvp_tier_config WHERE min_points <= GREATEST(0, pvp_weekly_ranking.points + excluded.points) ORDER BY min_points DESC LIMIT 1);
        """.trimIndent()

        try {
            _db.createQueryBuilder()
                .addStatement(sql, arrayOf(userId, week, year, gameMode, deltaPoint, if (isWinner) 1 else 0, if (isLoser) 1 else 0, deltaPoint))
                .executeUpdate()
        } catch (e: Exception) {
            _logger.error("Failed to update weekly ranking for user $userId", e)
        }
    }

    private fun updateMonthlyRanking(userId: Int, month: Int, year: Int, gameMode: String, deltaPoint: Int, isWinner: Boolean, isLoser: Boolean, wagered: Double, won: Double) {
        val sql = """
            INSERT INTO pvp_monthly_ranking (user_id, month, year, game_mode, points, wins, losses, total_wagered, total_won, tier)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, (SELECT tier FROM pvp_tier_config WHERE min_points <= ? ORDER BY min_points DESC LIMIT 1))
            ON CONFLICT (user_id, month, year, game_mode) DO UPDATE SET
                points = GREATEST(0, pvp_monthly_ranking.points + excluded.points),
                wins = pvp_monthly_ranking.wins + excluded.wins,
                losses = pvp_monthly_ranking.losses + excluded.losses,
                total_wagered = pvp_monthly_ranking.total_wagered + excluded.total_wagered,
                total_won = pvp_monthly_ranking.total_won + excluded.total_won,
                tier = (SELECT tier FROM pvp_tier_config WHERE min_points <= GREATEST(0, pvp_monthly_ranking.points + excluded.points) ORDER BY min_points DESC LIMIT 1);
        """.trimIndent()

        try {
            _db.createQueryBuilder()
                .addStatement(sql, arrayOf(userId, month, year, gameMode, deltaPoint, if (isWinner) 1 else 0, if (isLoser) 1 else 0, wagered, won, deltaPoint))
                .executeUpdate()
        } catch (e: Exception) {
            _logger.error("Failed to update monthly ranking for user $userId", e)
        }
    }
}
