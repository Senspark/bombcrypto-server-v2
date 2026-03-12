package com.senspark.game.service

import com.smartfoxserver.v2.db.IDBManager
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray
import java.sql.Connection
import java.sql.PreparedStatement

private fun PreparedStatement.executeUpdate(params: Array<out Any?>) {
    if (params.isNotEmpty()) {
        for (i in params.indices) {
            setObject(i + 1, params[i])
        }
    }
    executeUpdate()
}

class SmartfoxDatabase(private val _database: IDBManager, private val _log: (String?) -> Unit) {
    class DefaultQueryBuilder(private val _database: IDBManager, private val _log: (String?) -> Unit) {
        private val _statements = mutableListOf<Pair<String, Array<out Any?>>>()
        fun addStatement(statement: String, vararg args: Any?): DefaultQueryBuilder {
            _statements.add(Pair(statement, args))
            return this
        }

        fun executeQuery(): ISFSArray {
            val statement = _statements[0]
            return try {
                _database.executeQuery(statement.first, statement.second)
            } catch (ex: Exception) {
                log(statement.first, ex)
                SFSArray()
            }
        }

        fun executeUpdate() {
            if (_statements.size > 1) {
                executeMultiUpdate()
            } else {
                executeSingleUpdate()
            }
        }

        private fun executeMultiUpdate() {
            var connection: Connection? = null
            try {
                connection = _database.connection
                connection.autoCommit = false
                for (statement in _statements) {
                    val stmt = connection.prepareStatement(statement.first)
                    stmt.executeUpdate(statement.second)
                    stmt.close()
                }
                connection.commit()
            } catch (ex: Exception) {
                log(_statements.joinToString(", ") { it.first }, ex)
                connection?.rollback()
            } finally {
                connection?.close()
            }
        }

        private fun executeSingleUpdate() {
            val statement = _statements[0]
            try {
                _database.executeUpdate(statement.first, statement.second)
            } catch (ex: Exception) {
                log(statement.first, ex)
            }
        }

        private fun log(statement: String, ex: Exception) {
            _log("\n============DB_ERROR_LOG================\nstatement: $statement\nex${ex.message}\n============DB_ERROR_LOG================")
        }
    }

    fun createQueryBuilder(log: Boolean): DefaultQueryBuilder {
        return DefaultQueryBuilder(_database, _log)
    }
}