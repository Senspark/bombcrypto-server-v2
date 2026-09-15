package com.senspark.common.cache

import com.senspark.common.service.SimpleScheduler
import com.senspark.common.utils.ColorCode
import com.senspark.common.utils.ILogger
import io.lettuce.core.RedisClient
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Needs real Redis (REDIS_CONNECTION_STRING, default redis://localhost:6379); skipped if unreachable.
class FastStreamRedisTest {
    private val redisUrl = System.getenv("REDIS_CONNECTION_STRING") ?: "redis://localhost:6379"
    private val keys = List(12) { "TEST_FAST_STREAM_${it}_${UUID.randomUUID()}" }
    private var fastStreamRedis: FastStreamRedis? = null

    private val logger = object : ILogger {
        override fun log2(visibleTag: String, compressibleMessage: String, customColor: ColorCode) {}
        override fun log2(visibleTag: String, compressibleMessage: () -> String, customColor: ColorCode) {}
        override fun log(message: String, customColor: ColorCode) {}
        override fun warn(message: String) {}
        override fun error(message: String) = println("fastStreamRedis error: $message")
        override fun error(ex: Exception) = println("fastStreamRedis error: $ex")
        override fun error(prefix: String, ex: Exception) = println("fastStreamRedis error: $prefix $ex")
    }

    private fun redisReachable(): Boolean {
        val client = RedisClient.create(redisUrl).apply { setDefaultTimeout(Duration.ofSeconds(1)) }
        return try {
            client.connect().use { it.sync().ping() == "PONG" }
        } catch (e: Exception) {
            println("skipping: no Redis at $redisUrl ($e)")
            false
        } finally {
            client.shutdown()
        }
    }

    @AfterTest
    fun cleanup() {
        if (fastStreamRedis == null) return
        val client = RedisClient.create(redisUrl)
        client.connect().use { c -> keys.forEach { c.sync().del(it) } }
        client.shutdown()
        fastStreamRedis?.destroy()
    }

    @Test
    fun `delivers promptly on any of many listened streams, deletes confirmed entries, isolates a throwing listener`() {
        if (!redisReachable()) return
        val m = FastStreamRedis(RedisServices(redisUrl), SimpleScheduler(), logger)
        fastStreamRedis = m

        val throwingKey = keys[0]
        val target = keys.last()
        keys.dropLast(1).forEach { key -> m.listen(key) { error("boom on $key") } }
        val received = CountDownLatch(1)
        var value: String? = null
        m.listen(target) { message ->
            value = message.value
            received.countDown()
            true
        }
        m.initialize()

        // Warm up the read loop before measuring.
        Thread.sleep(300)
        m.send(throwingKey, "first a message whose listener throws")
        val sentAt = System.nanoTime()
        m.send(target, "payload")

        assertTrue(received.await(2, TimeUnit.SECONDS), "message never delivered")
        val latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - sentAt)
        assertEquals("payload", value)
        assertTrue(latencyMs < 500, "delivery took ${latencyMs}ms")

        val client = RedisClient.create(redisUrl)
        try {
            client.connect().use { c ->
                val deadline = System.currentTimeMillis() + 1000
                while (c.sync().xlen(target) != 0L && System.currentTimeMillis() < deadline) Thread.sleep(20)
                assertEquals(0L, c.sync().xlen(target), "a listener returning true deletes the entry")
                assertEquals(1L, c.sync().xlen(throwingKey), "a listener returning/throwing false keeps it")
            }
        } finally {
            client.shutdown()
        }
    }
}
