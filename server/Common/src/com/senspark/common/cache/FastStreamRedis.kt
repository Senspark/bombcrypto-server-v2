package com.senspark.common.cache

import com.senspark.common.service.IScheduler
import com.senspark.common.utils.ILogger
import com.senspark.common.utils.LazyMutable
import io.lettuce.core.*
import io.lettuce.core.api.StatefulRedisConnection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.collections.HashMap

class FastStreamRedis(
    private val _redis: IRedisServices,
    private val _scheduler: IScheduler,
    private val _logger: ILogger,
) : IFastStreamRedis {
    companion object {
        // One blocking XREAD across all streams per tick; keep this for a few hot streams only.
        const val SCHEDULER_TIME = 20
        private const val READ_BLOCK_MS = 200L
        private const val READ_MAX_COUNT = 100L
    }

    private val _listeners = ConcurrentHashMap<String, MutableList<(Message) -> Boolean>>()
    private val _latestIds = ConcurrentHashMap<String, String>()
    private val _connection: StatefulRedisConnection<String, String> by LazyMutable {
        _redis.getNewConnection()
    }

    // Dedicated read connection so send()/delete() don't queue behind the blocking XREAD.
    private val _readConnection: StatefulRedisConnection<String, String> by LazyMutable {
        _redis.getNewConnection()
    }

    override fun initialize() {
        _scheduler.schedule("FastStreamRedis", 0, SCHEDULER_TIME, ::listenToStreams)
    }

    override fun send(key: String, message: String) {
        try {
            val cmd = _connection.sync()
            val data = HashMap<String, String>()
            data["data"] = message
            cmd.xadd(key, data)
        } catch (ex: Exception) {
            _logger.error(ex)
        }
    }

    override fun listen(key: String, callback: (Message) -> Boolean) {
        _logger.log("FastStreamRedis listen to $key")
        if (!_listeners.containsKey(key)) {
            createStreamListener(key)
            _listeners[key] = CopyOnWriteArrayList()
        }
        _listeners[key]?.add(callback)
    }

    override fun delete(key: String, id: String) {
        try {
            val cmd = _connection.sync()
            cmd.xdel(key, id)
        } catch (ex: Exception) {
            _logger.error("[FAST_STREAM_REDIS] DELETE ERR: ${ex.message}")
        }
    }

    override fun destroy() {
        _listeners.clear()
        _latestIds.clear()
        _connection.close()
        _readConnection.close()
        _redis.dispose()
    }

    private fun createStreamListener(key: String) {
        _latestIds[key] = getLatestId(key)
    }

    private fun listenToStreams() {
        val offsets = _listeners.keys.mapNotNull { key ->
            _latestIds[key]?.let { XReadArgs.StreamOffset.from(key, it) }
        }
        if (offsets.isEmpty()) {
            return
        }
        try {
            val cmd = _readConnection.sync()
            val dataArr = cmd.xread(
                XReadArgs.Builder.block(READ_BLOCK_MS).count(READ_MAX_COUNT),
                *offsets.toTypedArray(),
            )
            for (data in dataArr) {
                val key = data.stream
                _latestIds[key] = data.id
                for (message in data.body) {
                    val dataKey = message.key // "data"
                    val dataValue = message.value // "content"
                    val msg = Message(
                        id = data.id,
                        key = dataKey,
                        value = dataValue
                    )
                    var confirmed = false
                    _listeners[key]?.forEach {
                        confirmed = confirmed || invokeListener(key, it, msg)
                    }
                    if (confirmed) {
                        delete(key, data.id)
                    }
                }
            }
        } catch (ex: Exception) {
            if (ex is RedisConnectionException) {
                // ignore
            } else {
                _logger.error("[FAST_STREAM_REDIS] LISTEN ERR: ${ex.message}")
            }
        }
    }

    // Streams share one read, so one listener's error must not drop other messages.
    private fun invokeListener(key: String, listener: (Message) -> Boolean, msg: Message): Boolean {
        return try {
            listener(msg)
        } catch (ex: Exception) {
            _logger.error("[FAST_STREAM_REDIS] LISTENER ERR $key ${msg.id}: ${ex.message}")
            false
        }
    }

    private fun getLatestId(streamKey: String): String {
        try {
            val cmd = _connection.sync()
            val data = cmd.xinfoStream(streamKey)
            val index = data.indexOf("last-generated-id")
            val latestId: String = data[index + 1].toString()
            return latestId
        } catch (ex: Exception) {
            _logger.error("[FAST_STREAM_REDIS] GET LATEST ID $streamKey ERR: ${ex.message}")
            return "0-0"
        }
    }
}
