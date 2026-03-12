package com.senspark.game.db

import com.senspark.common.IDatabase
import com.senspark.common.utils.ILogger
import com.senspark.lib.db.BaseDataAccess
import java.sql.Timestamp

open class LogDataAccessExecutor(
    database: IDatabase,
    log: Boolean,
    logger: ILogger
) : BaseDataAccess(database, log, logger) {

    fun logClaimReward(statement: String, userName: String, type: String, value: Float, apiStatus: String) {
        val logDate = Timestamp(System.currentTimeMillis())
        val params = arrayOf<Any?>(userName, type, value, logDate, apiStatus)
        executeUpdate(statement, params)
    }

    fun logRefundReward(statement: String, userName: String, type: String, value: Float, refundReason: String) {
        val logDate = Timestamp(System.currentTimeMillis())
        val params = arrayOf<Any?>(userName, type, value, refundReason, logDate)
        executeUpdate(statement, params)
    }

    fun logHack(statement: String, userName: String, hackType: Int, data: String) {
        val logDate = Timestamp(System.currentTimeMillis())
        val params = arrayOf<Any?>(userName, hackType, data, logDate)
        executeUpdate(statement, params)
    }
}