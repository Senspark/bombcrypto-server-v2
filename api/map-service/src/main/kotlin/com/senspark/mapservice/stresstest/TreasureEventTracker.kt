package com.senspark.mapservice.stresstest

import com.senspark.mapservice.model.TreasureEventBatch
import com.senspark.mapservice.redis.ChannelKeys
import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.RedisPubSubAdapter
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

// Subscribes to AP_MAP_TREASURE_EVENT_CHANNEL like the game server's router: registered sessions'
// batches are queued, everything else is ignored.
class TreasureEventTracker(
    private val redisConnectionString: String,
    private val metrics: Metrics,
) {
    data class Received(val batch: TreasureEventBatch, val receivedAtMs: Long)

    private val queues = ConcurrentHashMap<String, ConcurrentLinkedQueue<Received>>()
    private val json = Json { ignoreUnknownKeys = true }
    private var client: RedisClient? = null

    fun register(sessionKey: String): ConcurrentLinkedQueue<Received> =
        queues.getOrPut(sessionKey) { ConcurrentLinkedQueue() }

    fun unregister(sessionKey: String) {
        queues.remove(sessionKey)
    }

    fun start() {
        val redisClient = RedisClient.create(redisConnectionString)
        client = redisClient
        try {
            val connection = redisClient.connectPubSub()
            connection.addListener(object : RedisPubSubAdapter<String, String>() {
                override fun message(channel: String, message: String) = onMessage(message)
            })
            connection.sync().subscribe(ChannelKeys.AP_MAP_TREASURE_EVENT_CHANNEL)
        } catch (e: Exception) {
            System.err.println(
                "[treasure-tracker] could not connect to Redis ($redisConnectionString): ${e.message}; " +
                    "no treasure events will be received."
            )
            redisClient.shutdown()
            client = null
        }
    }

    // Runs on Lettuce's I/O thread: decode and enqueue only.
    private fun onMessage(data: String) {
        val batch = try {
            json.decodeFromString(TreasureEventBatch.serializer(), data)
        } catch (e: Exception) {
            metrics.endpoint("ev_decode").recordError()
            return
        }
        queues[batch.sessionKey]?.add(Received(batch, System.currentTimeMillis()))
    }

    fun stop() {
        client?.shutdown()
    }
}
