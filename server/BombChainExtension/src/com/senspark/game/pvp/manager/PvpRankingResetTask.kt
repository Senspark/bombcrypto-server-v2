package com.senspark.game.pvp.manager

import com.senspark.common.IDatabase
import com.senspark.common.utils.ILogger
import java.time.LocalDate
import java.time.temporal.IsoFields

class PvpRankingResetTask(
    private val _db: IDatabase,
    private val _logger: ILogger
) {
    fun run() {
        try {
            checkAndResetWeekly()
            checkAndResetMonthly()
        } catch (e: Exception) {
            _logger.error("Error in PvpRankingResetTask: ${e.message}", e)
        }
    }

    private fun checkAndResetWeekly() {
        val now = LocalDate.now()
        val currentWeek = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val currentYear = now.get(IsoFields.WEEK_BASED_YEAR)

        // Reset points to floor of current tier at the start of a new week
        // We track the last reset in a simple config table or check if rankings for current week exist
        // Industry best practice: use a pvp_ranking_reset_log table
        
        val sqlReset = """
            -- Only run if there are entries from PREVIOUS weeks that haven't been reset yet
            -- This logic is tricky. Simpler: Update points for all users in the current week table 
            -- IF it's a new week and we haven't initialized it.
            
            -- Better approach as per user: "Reset semanal: pontos caem para floor do tier"
            -- This usually applies to the NEW week's starting points.
            
            INSERT INTO pvp_weekly_ranking (user_id, week_number, year, game_mode, points, wins, losses, matches_played, tier)
            SELECT 
                user_id, 
                ? as week_number, 
                ? as year, 
                game_mode, 
                (SELECT reset_floor FROM pvp_tier_config WHERE tier = pvp_weekly_ranking.tier) as points,
                0, 0, 0,
                tier
            FROM pvp_weekly_ranking
            WHERE (week_number < ? OR year < ?)
            ON CONFLICT (user_id, week_number, year, game_mode) DO NOTHING;
        """.trimIndent()

        _db.createQueryBuilder().addStatement(sqlReset, arrayOf(currentWeek, currentYear, currentWeek, currentYear)).executeUpdate()
    }

    private fun checkAndResetMonthly() {
        val now = LocalDate.now()
        val currentMonth = now.monthValue
        val currentYear = now.year

        // Reset: points drop by 1 tier
        val sqlReset = """
            INSERT INTO pvp_monthly_ranking (user_id, month, year, game_mode, points, wins, losses, total_wagered, total_won, tier)
            SELECT 
                user_id, 
                ? as month, 
                ? as year, 
                game_mode, 
                -- Find min_points of the tier below current tier
                COALESCE((
                    SELECT min_points 
                    FROM pvp_tier_config 
                    WHERE min_points < (SELECT min_points FROM pvp_tier_config WHERE tier = pvp_monthly_ranking.tier)
                    ORDER BY min_points DESC 
                    LIMIT 1
                ), 0) as points,
                0, 0, 0, 0,
                -- Find tier name below current tier
                COALESCE((
                    SELECT tier 
                    FROM pvp_tier_config 
                    WHERE min_points < (SELECT min_points FROM pvp_tier_config WHERE tier = pvp_monthly_ranking.tier)
                    ORDER BY min_points DESC 
                    LIMIT 1
                ), 'BRONZE') as tier
            FROM pvp_monthly_ranking
            WHERE (month < ? OR year < ?)
            ON CONFLICT (user_id, month, year, game_mode) DO NOTHING;
        """.trimIndent()

        _db.createQueryBuilder().addStatement(sqlReset, arrayOf(currentMonth, currentYear, currentMonth, currentYear)).executeUpdate()
    }
}
