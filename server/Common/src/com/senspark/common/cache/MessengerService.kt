package com.senspark.common.cache

import com.senspark.common.service.IScheduler
import com.senspark.common.utils.ILogger
import com.senspark.common.utils.LazyMutable
import io.lettuce.core.*
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import kotlin.collections.HashMap

class MessengerService(
    private val _redis: IRedisServices,
    private val _scheduler: IScheduler,
    private val _logger: ILogger,
) : IMessengerService {
    companion object {
        const val STREAM_CONSUMER_GROUP = "sv-smartfox"
        const val SCHEDULER_TIME = 1000
        const val BUS_PING_INTERVAL = 60_000
    }

    private val _listeners = mutableMapOf<String, MutableList<(Message) -> Boolean>>()
    private val _latestIds = mutableMapOf<String, String>()
    private val _connection: StatefulRedisConnection<String, String> by LazyMutable {
        _redis.getNewConnection()
    }

    // _connection is held by the blocking XREAD loop; PUBLISH must not queue behind it.
    private val _publishConnection: StatefulRedisConnection<String, String> by lazy {
        _redis.getNewConnection()
    }

    // Subscribed connection can not run other commands, so the bus gets its own. Lettuce calls the listener on
    // its I/O thread, which must not block: callbacks run on a single dedicated thread, in arrival order.
    private val _busCallbacks = ConcurrentHashMap<Pair<String, String>, CopyOnWriteArrayList<(String) -> Unit>>()
    private val _busChannels = ConcurrentHashMap.newKeySet<String>()
    private val _busExecutor = Executors.newSingleThreadExecutor()
    private val _busConnection: StatefulRedisPubSubConnection<String, String> by lazy {
        _redis.getNewPubSubConnection().apply {
            addListener(object : RedisPubSubAdapter<String, String>() {
                override fun message(channel: String, message: String) {
                    _busExecutor.execute { dispatchBus(channel, message) }
                }
            })
        }
    }

    override fun initialize() {
        _scheduler.schedule("Messengers", 0, SCHEDULER_TIME, ::listenToStreams)
        // A subscribed connection can sit idle for weeks; proxies, firewalls and NATs drop idle TCP silently,
        // leaving a subscriber that never hears another message. Regular traffic keeps the path open.
        _scheduler.schedule("MessengerBusPing", BUS_PING_INTERVAL, BUS_PING_INTERVAL, ::pingBus)
    }

    private fun pingBus() {
        if (_busChannels.isEmpty()) return
        try {
            _busConnection.sync().ping()
        } catch (ex: Exception) {
            _logger.error("[MessengerService] bus ping failed", ex)
        }
    }

    override fun send(key: String, message: String, maxLen: Long?) {
        try {
            val cmd = _connection.sync()
            val data = HashMap<String, String>()
            data["data"] = message
            if (maxLen == null) {
                cmd.xadd(key, data)
            } else {
                cmd.xadd(key, XAddArgs.Builder.maxlen(maxLen).approximateTrimming(), data)
            }
        } catch (ex: Exception) {
            _logger.error(ex)
        }
    }

    override fun publish(channel: String, message: String) {
        try {
            _publishConnection.async().publish(channel, message).whenComplete { _, ex ->
                if (ex != null) {
                    _logger.error("[MESSENGER_SERVICE] PUBLISH $channel ERR: ${ex.message}")
                }
            }
        } catch (ex: Exception) {
            _logger.error("[MESSENGER_SERVICE] PUBLISH $channel ERR: ${ex.message}")
        }
    }

    override fun publishBus(channel: String, type: String, data: String) {
        val message = buildJsonObject {
            put("type", type)
            put("data", data)
        }.toString()
        publish(channel, message)
    }

    override fun onBus(channel: String, type: String, callback: (String) -> Unit) {
        _logger.log("Listen to $channel:$type")
        _busCallbacks.computeIfAbsent(Pair(channel, type)) { CopyOnWriteArrayList() }.add(callback)
        if (_busChannels.add(channel)) {
            _busConnection.sync().subscribe(channel)
        }
    }

    private fun dispatchBus(channel: String, message: String) {
        try {
            val envelope = Json.parseToJsonElement(message).jsonObject
            val type = envelope["type"]!!.jsonPrimitive.content
            val data = envelope["data"]!!.jsonPrimitive.content
            _busCallbacks[Pair(channel, type)]?.forEach {
                try {
                    it(data)
                } catch (ex: Exception) {
                    _logger.error("[MESSENGER_SERVICE] BUS $channel:$type ERR: ${ex.message}")
                }
            }
        } catch (ex: Exception) {
            _logger.error("[MESSENGER_SERVICE] BUS $channel bad message: ${ex.message}")
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
        if (_busChannels.isNotEmpty()) {
            _busConnection.close()
        }
        _busExecutor.shutdownNow()
        _publishConnection.close()
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
