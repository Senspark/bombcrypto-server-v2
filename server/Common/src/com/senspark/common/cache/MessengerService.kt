package com.senspark.common.cache

import com.senspark.common.service.IScheduler
import com.senspark.common.utils.ILogger
import com.senspark.common.utils.LazyMutable
import io.lettuce.core.*
import io.lettuce.core.api.StatefulRedisConnection
import kotlin.collections.HashMap

class MessengerService(
    private val _redis: IRedisServices,
    private val _scheduler: IScheduler,
    private val _logger: ILogger,
) : IMessengerService {
    companion object {
        const val STREAM_CONSUMER_GROUP = "sv-smartfox"
        const val SCHEDULER_TIME = 1000
    }

    private val _listeners = mutableMapOf<String, MutableList<(Message) -> Boolean>>()
    private val _latestIds = mutableMapOf<String, String>()
    private val _connection: StatefulRedisConnection<String, String> by LazyMutable {
        _redis.getNewConnection()
    }

    override fun initialize() {
        _scheduler.schedule("Messengers", 0, SCHEDULER_TIME, ::listenToStreams)
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
        _logger.log("Listen to $key")
        if (!_listeners.containsKey(key)) {
            _listeners[key] = mutableListOf()
            createStreamListener(key)
        }
        _listeners[key]?.add(callback)
    }
    
    override fun delete(key: String, id: String) {
        try {
            val cmd = _connection.sync()
            cmd.xdel(key, id)
            _logger.log("Deleted message with ID $id from stream $key")
        } catch (ex: Exception) {
            _logger.error("[MESSENGER_SERVICE] DELETE ERR: ${ex.message}")
        }
    }

    override fun destroy() {
        _listeners.clear()
        _latestIds.clear()
        _connection.close()
        _redis.dispose()
    }

    private fun createStreamListener(key: String) {
        _latestIds[key] = getLatestId(key)
    }

    private fun listenToStreams() {
        val keys = _listeners.keys.toList()
        keys.forEach {
            listenToStream(it)
        }
    }

    private fun listenToStream(key: String) {
        try {
            val readMaxCount = 100L
            val cmd = _connection.sync()
            val dataArr = cmd.xread(
                XReadArgs.Builder.block(200).count(readMaxCount),
                XReadArgs.StreamOffset.from(key, _latestIds[key]),
            )
            for (data in dataArr) {
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
                        confirmed = confirmed || it(msg)
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
                _logger.error("[MESSENGER_SERVICE] LISTEN ERR: ${ex.message}")
            }
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
            _logger.error("[MESSENGER_SERVICE] GET LATEST ID $streamKey ERR: ${ex.message}")
            return "0-0"
        }
    }
}