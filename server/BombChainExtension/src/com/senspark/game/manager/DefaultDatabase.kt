package com.senspark.game.manager

import com.senspark.common.IDatabase
import com.senspark.common.IQueryBuilder
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.JavaLocalDateTimeColumnType
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.ResultSet
import java.sql.Timestamp

class DefaultDatabase(
    override val database: Database,
    private val _logger: SqlLogger,
) : IDatabase {

    override fun initialize() {
    }

    override fun createQueryBuilder(enableLog: Boolean): IQueryBuilder {
        return DefaultQueryBuilder(database, _logger, enableLog)
    }

    private class DefaultQueryBuilder(
        private val _db: Database,
        private val _logger: SqlLogger,
        private val _enableLog: Boolean
    ) : IQueryBuilder {
        private val _statements = mutableListOf<Triple<String, Array<Any?>, StatementType>>()

        override fun addStatement(statement: String, args: Array<Any?>): DefaultQueryBuilder {
            _statements.add(Triple(shortenSql(statement), args, StatementType.SELECT))
            return this
        }

        override fun addStatementUpdate(statement: String, args: Array<Any?>): DefaultQueryBuilder {
            _statements.add(Triple(shortenSql(statement), args, StatementType.UPDATE))
            return this
        }

        override fun executeQuery(): ISFSArray {
            require(_statements.size <= 1)
            var result: ISFSArray = SFSArray.newInstance()
            transaction(_db) {
                if (_enableLog) {
                    addLogger(_logger)
                }
                _statements.forEach { entry ->
                    exec(entry.first, parseArgs(entry.second), StatementType.SELECT) {
                        result = SFSArray.newFromResultSet(it)
                    }
                }
            }
            return result
        }

        override fun <T : Any> executeQuery(transform: (ResultSet) -> T) {
            require(_statements.size <= 1)
            transaction(_db) {
                if (_enableLog) {
                    addLogger(_logger)
                }
                _statements.forEach { entry ->
                    exec(entry.first, parseArgs(entry.second), StatementType.SELECT) {
                        while (it.next())
                            transform(it)
                    }
                }
            }
        }

        override fun executeUpdate() {
            transaction(_db) {
                if (_enableLog) {
                    addLogger(_logger)
                }
                try {
                    _statements.forEach { entry ->
                        exec(entry.first, parseArgs(entry.second), StatementType.UPDATE)
                    }
                } catch (ex: Exception) {
                    TransactionManager.current().rollback()
                    throw ex
                }
            }
        }

        override fun executeMultiQuery() {
            transaction(_db) {
                if (_enableLog) {
                    addLogger(_logger)
                }
                try {
                    _statements.forEach { entry ->
                        exec(entry.first, parseArgs(entry.second), entry.third)
                    }
                } catch (ex: Exception) {
                    TransactionManager.current().rollback()
                    throw ex
                }
            }
        }

        private fun shortenSql(statement: String): String {
            return statement
                .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "") // Remove multi-line comments
                .replace(Regex("--.*?$", RegexOption.MULTILINE), "") // Remove single-line comments
                .replace("\n", " ")
                .replace("\t", " ")
                .replace(Regex("\\s+"), " ")
        }

        private fun parseArgs(args: Array<Any?>): List<Pair<ColumnType, Any?>> {
            return args.map {
                val columnType = when (it) {
                    is Boolean -> BooleanColumnType()
                    is Int -> IntegerColumnType()
                    is Long -> LongColumnType()
                    is Float -> FloatColumnType()
                    is Double -> DoubleColumnType()
                    is String -> VarCharColumnType()
                    is Timestamp -> JavaLocalDateTimeColumnType()
                    is ByteArray -> BinaryColumnType(Int.MAX_VALUE)
                    is Boolean? -> BooleanColumnType().apply { nullable = true }
                    is Int? -> IntegerColumnType().apply { nullable = true }
                    is Long? -> LongColumnType().apply { nullable = true }
                    is Float? -> FloatColumnType().apply { nullable = true }
                    is Double? -> DoubleColumnType().apply { nullable = true }
                    is String? -> VarCharColumnType().apply { nullable = true }
                    else -> throw IllegalArgumentException("Unknown column type of $it")
                }
                Pair(columnType, it)
            }
        }
    }
}