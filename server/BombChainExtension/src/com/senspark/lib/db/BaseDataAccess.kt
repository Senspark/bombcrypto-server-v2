package com.senspark.lib.db

import com.senspark.common.IDatabase
import com.senspark.common.utils.ILogger
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray
import java.sql.ResultSet
import java.sql.SQLException

open class BaseDataAccess(
    protected val database: IDatabase,
    private val log: Boolean,
    private val _logger: ILogger,
) {

    private fun logDbError(statement: String, ex: Throwable) {
        val strBuilder = StringBuilder()
        strBuilder.append("[SQL_ERROR]:")
        strBuilder.append("\nstatement: $statement")
        strBuilder.append("\nex: ${ex.message}")
        _logger.error(strBuilder.toString())
    }

    protected fun executeQuery(statement: String, params: Array<Any?>): ISFSArray {
        try {
            return database.createQueryBuilder().addStatement(statement, params).executeQuery()
        } catch (ex: Exception) {
            logDbError(statement, ex)
        }
        return SFSArray.newInstance()
    }

    protected fun <T : Any> executeQuery(statement: String, params: Array<Any?>, transform: (ResultSet) -> T) {
        try {
            database.createQueryBuilder().addStatement(statement, params).executeQuery(transform)
        } catch (ex: Exception) {
            logDbError(statement, ex)
        }
    }

    protected fun executeQueryAndThrowException(statement: String, params: Array<Any?>): ISFSArray {
        return database.createQueryBuilder().addStatement(statement, params).executeQuery()
    }

    protected fun <T : Any> executeQueryAndThrowException(statement: String, params: Array<Any?>, transform: (ResultSet) -> T) {
        database.createQueryBuilder().addStatement(statement, params).executeQuery(transform)
    }

    protected fun executeUpdate(statement: String, params: Array<Any?>, log: Boolean = false): Boolean {
        try {
            database.createQueryBuilder(log).addStatement(statement, params).executeUpdate()
            return true
        } catch (ex: Exception) {
            logDbError(statement, ex)
        }
        return false
    }

    @Throws(SQLException::class)
    protected fun executeUpdateReturnDataThrowException(statement: String, params: Array<Any?>): ISFSArray {
        return database.createQueryBuilder().addStatement(statement, params).executeQuery()
    }

    @Throws(SQLException::class)
    protected fun executeUpdateThrowException(statement: String, params: Array<Any?>) {
        database.createQueryBuilder(log).addStatement(statement, params).executeUpdate()
    }

    @Throws(SQLException::class)
    protected fun executeUpdateMultiQueryThrowException(pair: List<Pair<String, Array<Any?>>>) {
        val createQueryBuilder = database.createQueryBuilder(log)
        pair.forEach {
            createQueryBuilder.addStatement(it.first, it.second)
        }
        createQueryBuilder.executeUpdate()
    }

    protected fun executeUpdateReturnValue(statement: String, params: Array<Any?>): ISFSArray {
        try {
            return database.createQueryBuilder().addStatement(statement, params).executeQuery()
        } catch (ex: Exception) {
            logDbError(statement, ex)
        }
        return SFSArray.newInstance()
    }

    protected fun executeInsert(statement: String, params: Array<Any?>) {
        val func = "executeInsert"
        try {
            database.createQueryBuilder().addStatement(statement, params).executeUpdate()
        } catch (ex: Exception) {
            logDbError(statement, ex)
        }
    }

    private fun generateMessage(vararg params: Any?): String {
        val result = StringBuilder()
        for (param in params) {
            result.append(param).append("\t")
        }
        return result.toString()
    }
}