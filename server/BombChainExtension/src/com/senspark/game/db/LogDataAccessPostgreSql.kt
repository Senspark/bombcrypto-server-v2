package com.senspark.game.db

import com.senspark.common.IDatabase
import com.senspark.common.utils.ILogger
import com.senspark.game.constant.Booster
import com.senspark.game.declare.EnumConstants
import com.senspark.lib.db.BaseDataAccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LogDataAccessPostgreSql(
    database: IDatabase,
    log: Boolean,
    logger: ILogger,
    private val executor: LogDataAccessExecutor = LogDataAccessExecutor(database, log, logger)
) : BaseDataAccess(database, log, logger), ILogDataAccess {

    override fun initialize() {
    }
    
    override fun logClaimReward(userName: String, type: String, value: Float, apiStatus: String) {
        val statement =
            """INSERT INTO "log_claim_reward" ("user_name", "type", "value", "log_date", "api_status") VALUES(?,?,?,?,?);"""
        executor.logClaimReward(statement, userName, type, value, apiStatus)
    }

    override fun logRefundReward(userName: String, type: String, value: Float, refundReason: String) {
        val statement =
            """INSERT INTO "log_refund_reward" ("user_name", "type", "value", "refund_reason", "log_date") VALUES(?,?,?,?,?);"""
        executor.logRefundReward(statement, userName, type, value, refundReason)
    }

    override fun logHack(userName: String, hackType: Int, data: String) {
        val statement = """INSERT INTO "log_hack" ("user_name", "hack_type", "data", "log_date") VALUES(?,?,?,?);"""
        executor.logHack(statement, userName, hackType, data)
    }

    override fun logPlayPve(
        uid: Int,
        stage: Int,
        level: Int,
        result: EnumConstants.MatchResult,
        totalTime: Long,
        boosters: Map<Booster, Int>,
        heroId: Int,
    ) {
        val statement = """
        INSERT INTO log_play_pve(user_id, level, stage, match_time, match_result, total_time, boosters, hero_id)
        VALUES (?,
                ?,
                ?,
                NOW() AT TIME ZONE 'utc',
                ?,
                ?,
                CASE WHEN ? = '' THEN NULL ELSE ?::jsonb END,
                ?);
        """.trimIndent()
        val boostersFiltered = boosters.filter { it.value > 0 }
        val jsonBooster = if (boostersFiltered.isEmpty()) "" else Json.encodeToString(boostersFiltered)
        val params = arrayOf<Any?>(uid, level, stage, result.name, totalTime, jsonBooster, jsonBooster, heroId)
        executeUpdateThrowException(statement, params)
    }
}