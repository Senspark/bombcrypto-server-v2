package com.senspark.common.cache

import com.senspark.common.service.IScheduler
import com.senspark.common.utils.ILogger
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands

class RedisServices(connectionString: String) : IRedisServices {

    companion object {
        fun createCache(connectionString: String, logger: ILogger): ICacheService {
            val r = RedisServices(connectionString)
            return CacheRedis(r, logger)
        }

        fun createMessenger(connectionString: String, scheduler: IScheduler, logger: ILogger): IMessengerService {
            val r = RedisServices(connectionString)
            return MessengerService(r, scheduler, logger)
        }

        fun create(
            connectionString: String,
            scheduler: IScheduler,
            logger: ILogger
        ): Pair<ICacheService, IMessengerService> {
            val r = RedisServices(connectionString)
            return Pair(CacheRedis(r, logger), MessengerService(r, scheduler, logger))
        }
    }

    private val _client: RedisClient = RedisClient.create(connectionString)

    override fun getNewConnection(): StatefulRedisConnection<String, String> {
        return _client.connect()
    }

    override fun dispose() {
        _client.shutdown()
    }
}

interface IRedisServices {
    fun getNewConnection(): StatefulRedisConnection<String, String>
    fun dispose()
}