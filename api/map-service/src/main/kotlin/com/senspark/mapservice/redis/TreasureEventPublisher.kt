package com.senspark.mapservice.redis

import com.senspark.mapservice.model.TreasureEventBatch
import io.lettuce.core.LettuceFutures
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Pub/Sub channel names shared with bombcrypto-server-v2's `com.senspark.game.constant.ChannelKeys` --
 * keep both in lockstep. Naming follows that file's convention: `AP_` = published by a backend
 * service (here MapService), consumed by the SmartFox game server.
 */
object ChannelKeys {
    // Server-driven treasure mode: ordered MOVE/PLANT/EXPLODE/... batches per session.
    const val AP_MAP_TREASURE_EVENT_CHANNEL = "AP_MAP_TREASURE_EVENT_CHANNEL"
}

private val PUBLISH_TIMEOUT: Duration = Duration.ofSeconds(2)

fun interface TreasureEventPublisher {
    fun publish(batch: TreasureEventBatch)
}

/**
 * PUBLISHes the raw JSON on a Redis Pub/Sub channel: nothing is stored, a batch nobody is subscribed
 * for is simply gone. Redis being down never throws: the message is dropped and a warning logged.
 */
open class RedisPubSubPublisher(
    private val redis: RedisConnector,
    private val channel: String,
) {
    private val log = LoggerFactory.getLogger(RedisPubSubPublisher::class.java)
    protected val json = Json { encodeDefaults = true }

    /** Returns the number of subscribers that got the message, or null when Redis is unavailable. */
    protected fun publishData(data: String): Long? {
        val connection = redis.connection
        if (connection == null) {
            log.warn("redis not connected, dropped message channel={} bytes={}", channel, data.length)
            return null
        }
        return try {
            val publish = connection.async().publish(channel, data)
            LettuceFutures.awaitAll(PUBLISH_TIMEOUT, publish)
            publish.get()
        } catch (e: Exception) {
            log.warn("redis publish failed, dropped message channel={} bytes={}: {}", channel, data.length, e.toString())
            null
        }
    }
}

class RedisPubSubTreasureEventPublisher(
    redis: RedisConnector,
    channel: String = ChannelKeys.AP_MAP_TREASURE_EVENT_CHANNEL,
) : RedisPubSubPublisher(redis, channel), TreasureEventPublisher {
    // Nulls omitted: each event type only fills its own fields.
    @OptIn(ExperimentalSerializationApi::class)
    private val compactJson = Json { encodeDefaults = true; explicitNulls = false }

    override fun publish(batch: TreasureEventBatch) {
        publishData(compactJson.encodeToString(batch))
    }
}
