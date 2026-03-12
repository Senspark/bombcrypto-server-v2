package com.senspark.game.manager

import com.senspark.common.utils.AppStage
import com.senspark.common.utils.RemoteLoggerInitData
import java.time.Instant

class PvpEnvManager : IPvpEnvManager {
    override val isGke: Boolean = getEnv("GKE").toInt() == 1
    override val appStage = AppStage.fromString(getEnv("APP_STAGE", "LOCAL"))
    override val serverId: String = getEnv("SERVER_ID")
    override val apiUrl: String = getEnv("URL_REQUEST_PVP")

    override val postgresDriverName = "org.postgresql.Driver"
    override val postgresConnectionString = getEnv("POSTGRES_CONNECTION_STRING")
    override val postgresUsername = getEnv("POSTGRES_USERNAME")
    override val postgresPassword = getEnv("POSTGRES_PASSWORD")
    override val postgresMaxActiveConnections = getEnv("POSTGRES_MAX_ACTIVE_CONNECTIONS").toInt()
    override val postgresTestSql = "SELECT 1"

    override val redisConnectionString: String = getEnv("REDIS_CONNECTION_STRING")
    override val redisConsumerId: String = getEnv("SERVER_NAME", generateRandomString())

    override val logRemoteData = RemoteLoggerInitData(
        serviceName = "sv-bomb",
        instanceId = serverId,
        stage = appStage,
        remoteHost = getEnv("LOG_REMOTE_HOST", "localhost:10002")
    )

    override fun initialize() {
    }

    private fun getEnv(key: String, defaultValue: String? = null): String {
        return System.getenv(key)
            ?: defaultValue
            ?: throw IllegalArgumentException("Key not found: $key")
    }

    private fun generateRandomString(): String {
        return Instant.now().epochSecond.toString()
    }
}