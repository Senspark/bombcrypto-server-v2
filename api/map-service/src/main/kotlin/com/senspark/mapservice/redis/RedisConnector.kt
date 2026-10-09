package com.senspark.mapservice.redis

import io.lettuce.core.ClientOptions
import io.lettuce.core.RedisChannelHandler
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisConnectionStateListener
import io.lettuce.core.SocketOptions
import io.lettuce.core.TimeoutOptions
import io.lettuce.core.api.StatefulRedisConnection
import org.slf4j.LoggerFactory
import java.net.SocketAddress
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * One shared Redis connection that never takes MapService down: the first connect is retried in the
 * background until Redis is up, later drops are handled by Lettuce's auto-reconnect.
 */
class RedisConnector(
    connectionString: String,
    private val retryDelay: Duration = Duration.ofSeconds(3),
) : AutoCloseable {
    private val log = LoggerFactory.getLogger(RedisConnector::class.java)
    private val timeout = Duration.ofSeconds(2)

    private val client = RedisClient.create(connectionString).apply {
        setDefaultTimeout(timeout)
        // REJECT_COMMANDS: fail fast while disconnected instead of buffering publishes without bound.
        options = ClientOptions.builder()
            .autoReconnect(true)
            .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
            .socketOptions(SocketOptions.builder().connectTimeout(timeout).build())
            .timeoutOptions(TimeoutOptions.enabled(timeout))
            .build()
        addListener(object : RedisConnectionStateListener {
            override fun onRedisConnected(connection: RedisChannelHandler<*, *>?, socketAddress: SocketAddress?) {
                log.info("redis connected {}", socketAddress)
            }

            override fun onRedisDisconnected(connection: RedisChannelHandler<*, *>?) {
                if (!closed) log.warn("redis disconnected, reconnecting in background")
            }
        })
    }

    @Volatile
    private var closed = false
    private val connectedLatch = CountDownLatch(1)

    @Volatile
    var connection: StatefulRedisConnection<String, String>? = null
        private set

    private val connectThread = Thread(::connectLoop, "redis-connect").apply {
        isDaemon = true
        start()
    }

    private fun connectLoop() {
        var attempt = 0
        while (!closed) {
            attempt++
            try {
                connection = client.connect()
                connectedLatch.countDown()
                if (closed) connection?.close()
                return
            } catch (e: Exception) {
                if (closed) return
                log.warn("redis connect failed (attempt {}), retrying in {} ms: {}", attempt, retryDelay.toMillis(), e.message)
            }
            try {
                Thread.sleep(retryDelay.toMillis())
            } catch (_: InterruptedException) {
                return
            }
        }
    }

    fun awaitConnected(timeout: Duration): Boolean = connectedLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS)

    override fun close() {
        closed = true
        connectThread.interrupt()
        runCatching { connection?.close() }
        runCatching { client.shutdown() }
    }
}
