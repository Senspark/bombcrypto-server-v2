package com.senspark.common.cache

import com.senspark.common.utils.ILogger
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

class FastStreamRedis(
    private val _redis: IRedisServices,
    private val _logger: ILogger,
) : IFastStreamRedis {
    private val _listeners = ConcurrentHashMap<String, CopyOnWriteArrayList<(String) -> Unit>>()

    // Lettuce calls the listener on its I/O thread, which must not block.
    private val _executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "fast-stream-redis").apply { isDaemon = true }
    }
    private val _publishConnection: Lazy<StatefulRedisConnection<String, String>> = lazy {
        _redis.getNewConnection()
    }

    // A subscribed connection can not run other commands, so it gets its own.
    private val _subscribeConnection: Lazy<StatefulRedisPubSubConnection<String, String>> = lazy {
        _redis.getNewPubSubConnection().apply {
            addListener(object : RedisPubSubAdapter<String, String>() {
                override fun message(channel: String, message: String) {
                    _executor.execute { dispatch(channel, message) }
                }
            })
        }
    }

    override fun initialize() {
    }

    override fun send(key: String, message: String) {
        try {
            _publishConnection.value.sync().publish(key, message)
        } catch (ex: Exception) {
            _logger.error("[FAST_STREAM_REDIS] PUBLISH $key ERR: ${ex.message}")
        }
    }

    override fun listen(key: String, callback: (String) -> Unit) {
        _logger.log("FastStreamRedis listen to $key")
        var isNew = false
        _listeners.computeIfAbsent(key) {
            isNew = true
            CopyOnWriteArrayList()
        }.add(callback)
        if (isNew) {
            try {
                _subscribeConnection.value.sync().subscribe(key)
            } catch (ex: Exception) {
                _logger.error("[FAST_STREAM_REDIS] SUBSCRIBE $key ERR: ${ex.message}")
            }
        }
    }

    override fun destroy() {
        _listeners.clear()
        _executor.shutdownNow()
        if (_subscribeConnection.isInitialized()) {
            _subscribeConnection.value.close()
        }
        if (_publishConnection.isInitialized()) {
            _publishConnection.value.close()
        }
        _redis.dispose()
    }

    // Channels share one thread, so one listener's error must not drop other messages.
    private fun dispatch(channel: String, message: String) {
        _listeners[channel]?.forEach {
            try {
                it(message)
            } catch (ex: Exception) {
                _logger.error("[FAST_STREAM_REDIS] LISTENER ERR $channel: ${ex.message}")
            }
        }
    }
}
