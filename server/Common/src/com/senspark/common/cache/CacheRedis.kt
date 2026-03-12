package com.senspark.common.cache

import com.senspark.common.utils.ILogger
import com.senspark.common.utils.LazyMutable
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.multi
import java.time.Instant
import kotlin.time.Duration

class CacheRedis(
    private val _redis: IRedisServices,
    private val _logger: ILogger,
) : ICacheService {

    private val _connection: StatefulRedisConnection<String, String> by LazyMutable {
        _redis.getNewConnection()
    }

    override fun initialize() {
    }

    override fun test() {
        val result = _connection.sync().ping()
        _logger.log("Redis ping result: $result")
    }

    override fun get(key: String): String? {
        return _connection.sync().get(key)
    }

    override fun set(key: String, value: String) {
        _connection.sync().set(key, value)
    }

    override fun set(key: String, values: List<String>) {
        val cmd = _connection.sync()
        cmd.del(key)
        cmd.rpush(key, *values.toTypedArray())
    }

    override fun set(key: String, value: String, ttl: Duration) {
        _connection.sync().set(key, value, SetArgs.Builder.ex(ttl.inWholeSeconds))
    }

    override fun set(key: String, values: List<String>, ttl: Duration) {
        val cmd = _connection.sync()
        cmd.del(key)
        cmd.rpush(key, *values.toTypedArray())
        cmd.expire(key, ttl.inWholeSeconds)
    }

    override fun delete(key: String) {
        _connection.sync().del(key)
    }

    override fun getFromHash(key: String, field: String): String? {
        val cmd = _connection.sync()
        val value = cmd.hget(key, field) ?: return null
        val parts = value.split("<EX>")
        return if (parts.size == 1) {
            parts[0]
        } else {
            val expiredAt = parts[1].toLong()
            val epochNow = Instant.now().toEpochMilli() / 1000
            if (expiredAt < epochNow) {
                cmd.hdel(key, field)
                null
            } else {
                parts[0]
            }
        }
    }

    override fun getAllFromHash(key: String): Map<String, String?> {
        val cmd = _connection.sync()
        val result = cmd.hgetall(key)
        val epochNow = Instant.now().toEpochMilli() / 1000

        return result.mapValues { entry ->
            val parts = entry.value.split("<EX>")
            if (parts.size == 1) {
                parts[0]
            } else {
                val expiredAt = parts[1].toLong()
                if (expiredAt < epochNow) {
                    cmd.hdel(key, entry.key)
                    null
                } else {
                    parts[0]
                }
            }
        }
    }

    override fun setToHash(key: String, field: String, value: String) {
        _connection.sync().hset(key, field, value)
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    override fun setToHash(key: String, field: String, value: String, ttl: Duration) {
        _connection.sync().multi {
            hset(key, field, value)
            hexpire(key, ttl.inWholeSeconds, field)
        }
    }

    override fun deleteFromHash(key: String, field: String) {
        _connection.sync().hdel(key, field)
    }

    override fun isExistFromSet(key: String, field: String): Boolean {
        return _connection.sync().sismember(key, field)
    }

    override fun addToSet(key: String, field: String) {
        _connection.sync().sadd(key, field)
    }

    override fun readSet(key: String): Set<String> {
        return try {
            _connection.sync().smembers(key)
        } catch (ex: Exception) {
            emptySet()
        }
    }

    override fun deleteFromSet(key: String, field: String) {
        _connection.sync().srem(key, field)
    }

    override fun destroy() {
        _connection.close()
        _redis.dispose()
    }
}