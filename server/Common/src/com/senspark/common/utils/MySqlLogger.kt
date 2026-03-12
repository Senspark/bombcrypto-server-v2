package com.senspark.common.utils

import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.expandArgs

class MySqlLogger(
    private val ext: ILogger,
    private val _enableLog: Boolean,
) : SqlLogger {
    override fun log(context: StatementContext, transaction: Transaction) {
        if (!_enableLog) return
        val sql = context.expandArgs(transaction)
        ext.log("[SQL]: $sql", ColorCode.CYAN)
    }
}