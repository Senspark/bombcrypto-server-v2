package com.senspark.game.utils

import com.senspark.game.manager.IEnvManager
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

object DatabaseUtils {
    fun create(
        jdbcUrl: String,
        driverClassName: String,
        username: String,
        password: String,
        maximumPoolSize: Int,
        testQuery: String = "SELECT 1"
    ): Database {
//        println("Creating database connection with url: $jdbcUrl")
        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.driverClassName = driverClassName
            this.username = username
            this.password = password
            this.maximumPoolSize = maximumPoolSize
            this.connectionTestQuery = testQuery
            initializationFailTimeout = 5000
        }
        val source = HikariDataSource(config)
        return Database.connect(source)
    }

    fun create(envManager: IEnvManager): Database {
        return create(
            envManager.postgresConnectionString,
            "org.postgresql.Driver",
            envManager.postgresUsername,
            envManager.postgresPassword,
            envManager.postgresMaxActiveConnections,
            envManager.postgresTestSql,
        )
    }
}