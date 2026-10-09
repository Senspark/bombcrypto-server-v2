package com.senspark.mapservice.routes

import com.senspark.mapservice.mapServiceModule
import com.senspark.mapservice.model.AutoHeroDto
import com.senspark.mapservice.model.AutoHeroesRequest
import com.senspark.mapservice.model.AutoKeepaliveResponse
import com.senspark.mapservice.model.AutoSnapshotDto
import com.senspark.mapservice.model.AutoStartRequest
import com.senspark.mapservice.model.TreasureEventBatch
import com.senspark.mapservice.model.TreasureEventDto
import com.senspark.mapservice.model.TreasureEventType
import com.senspark.mapservice.redis.TreasureEventPublisher
import com.senspark.mapservice.testHero
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutoRoutesTest {
    private val key = "9-TR-PVE_V2"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private class RecordingTreasurePublisher : TreasureEventPublisher {
        val batches = CopyOnWriteArrayList<TreasureEventBatch>()
        val events get() = batches.flatMap { it.events }
        override fun publish(batch: TreasureEventBatch) {
            batches.add(batch)
        }
    }

    private suspend fun HttpClient.postJson(path: String, body: String) = post(path) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private suspend fun RecordingTreasurePublisher.await(timeoutMs: Long = 3000, until: (List<TreasureEventDto>) -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!until(events) && System.currentTimeMillis() < deadline) delay(10)
    }

    private val fastHero = AutoHeroDto(testHero(1), speed = 20, bombCount = 1)

    private suspend fun HttpClient.initAndStart(blocks: String, fuseMs: Long = 100): AutoSnapshotDto {
        assertEquals(HttpStatusCode.OK, postJson("/sessions/$key/init", """{"blocks":$blocks}""").status)
        val start = postJson("/sessions/$key/auto/start", json.encodeToString(AutoStartRequest(listOf(fastHero), fuseMs, mapResetPauseMs = 0)))
        assertEquals(HttpStatusCode.OK, start.status)
        return json.decodeFromString(start.bodyAsText())
    }

    @Test
    fun `start streams move, plant and explode batches in seq order`() = testApplication {
        val publisher = RecordingTreasurePublisher()
        application { mapServiceModule(publisher, 3000) }

        val snapshot = client.initAndStart("""[{"i":6,"j":0,"type":3,"hp":1,"maxHp":1},{"i":20,"j":16,"type":3,"hp":99,"maxHp":99}]""")
        assertEquals(0, snapshot.seq)
        assertEquals(1, snapshot.heroes.size)
        assertEquals(2, snapshot.blocks.size)

        publisher.await { es -> es.any { it.type == TreasureEventType.EXPLODE } }
        val events = publisher.events
        assertTrue(events.any { it.type == TreasureEventType.PLANT })
        assertEquals((1L..events.size).toList(), events.map { it.seq }, "one runner publishes every seq, in order")
        assertTrue(publisher.batches.all { it.sessionKey == key })
    }

    @Test
    fun `roster edits, map replace and stop are all reflected in the stream`() = testApplication {
        val publisher = RecordingTreasurePublisher()
        application { mapServiceModule(publisher, 3000) }
        client.initAndStart("""[{"i":6,"j":0,"type":3,"hp":99,"maxHp":99}]""", fuseMs = 60_000)

        val edit = AutoHeroesRequest(upsert = listOf(AutoHeroDto(testHero(2), 5, 1)), remove = listOf(1), reason = "energy")
        assertEquals(HttpStatusCode.OK, client.postJson("/sessions/$key/auto/heroes", json.encodeToString(edit)).status)
        publisher.await { es -> es.any { it.type == TreasureEventType.HERO_JOIN } }
        assertTrue(publisher.events.any { it.type == TreasureEventType.HERO_LEAVE && it.heroId == 1 && it.reason == "energy" })

        client.postJson("/sessions/$key/map", """{"blocks":[{"i":10,"j":4,"type":3,"hp":1,"maxHp":1}]}""")
        publisher.await { es -> es.any { it.type == TreasureEventType.NEW_MAP } }
        assertEquals(listOf(2), publisher.events.last { it.type == TreasureEventType.NEW_MAP }.heroes!!.map { it.heroId })

        val alive = json.decodeFromString<AutoKeepaliveResponse>(client.postJson("/sessions/$key/auto/keepalive", "{}").bodyAsText())
        assertTrue(alive.running)

        assertEquals(HttpStatusCode.OK, client.postJson("/sessions/$key/auto/stop", "{}").status)
        publisher.await { es -> es.any { it.type == TreasureEventType.HERO_LEAVE && it.reason == "stopped" } }
        val stopped = json.decodeFromString<AutoKeepaliveResponse>(client.postJson("/sessions/$key/auto/keepalive", "{}").bodyAsText())
        assertTrue(!stopped.running)
        assertEquals(HttpStatusCode.Conflict, client.postJson("/sessions/$key/auto/heroes", json.encodeToString(edit)).status)
    }

    @Test
    fun `pause halts the heroes until resumed and keepalive reports it`() = testApplication {
        val publisher = RecordingTreasurePublisher()
        application { mapServiceModule(publisher, 3000) }
        assertEquals(HttpStatusCode.NotFound, client.postJson("/sessions/$key/auto/pause", """{"paused":true}""").status)
        client.initAndStart("""[{"i":30,"j":16,"type":3,"hp":99,"maxHp":99}]""", fuseMs = 60_000)

        assertEquals(HttpStatusCode.OK, client.postJson("/sessions/$key/auto/pause", """{"paused":true}""").status)
        // A 20 tiles/s hero stops within one step; nothing happens after that.
        publisher.await { es -> es.any { it.type == TreasureEventType.MOVE && it.path!!.isEmpty() } }
        val halted = publisher.events.size
        delay(300)
        assertEquals(halted, publisher.events.size)
        val alive = json.decodeFromString<AutoKeepaliveResponse>(client.postJson("/sessions/$key/auto/keepalive", "{}").bodyAsText())
        assertTrue(alive.paused)

        assertEquals(HttpStatusCode.OK, client.postJson("/sessions/$key/auto/pause", """{"paused":false}""").status)
        publisher.await { es -> es.size > halted }
        assertEquals(TreasureEventType.MOVE, publisher.events[halted].type)

        client.postJson("/sessions/$key/auto/stop", "{}")
        assertEquals(HttpStatusCode.Conflict, client.postJson("/sessions/$key/auto/pause", """{"paused":true}""").status)
    }

    @Test
    fun `a game the game server stopped calling is stopped, and start revives it`() = testApplication {
        val publisher = RecordingTreasurePublisher()
        application { mapServiceModule(publisher, 3000, autoLeaseMs = 400) }
        client.initAndStart("""[{"i":30,"j":16,"type":3,"hp":99,"maxHp":99}]""", fuseMs = 50)

        // Keepalives hold the lease past its length.
        repeat(4) {
            delay(150)
            val alive = json.decodeFromString<AutoKeepaliveResponse>(client.postJson("/sessions/$key/auto/keepalive", "{}").bodyAsText())
            assertTrue(alive.running)
        }

        publisher.await { es -> es.any { it.type == TreasureEventType.HERO_LEAVE && it.reason == "stopped" } }
        val stopped = json.decodeFromString<AutoKeepaliveResponse>(client.postJson("/sessions/$key/auto/keepalive", "{}").bodyAsText())
        assertTrue(!stopped.running)
        delay(200)
        val count = publisher.events.size
        delay(300)
        assertEquals(count, publisher.events.size, "nothing is played once the lease is gone")

        val start = client.postJson("/sessions/$key/auto/start", json.encodeToString(AutoStartRequest(listOf(fastHero), 50, mapResetPauseMs = 0)))
        assertEquals(HttpStatusCode.OK, start.status)
        publisher.await { es -> es.size > count }
        assertTrue(publisher.events.size > count)
    }

    @Test
    fun `deleting the session stops the stream`() = testApplication {
        val publisher = RecordingTreasurePublisher()
        application { mapServiceModule(publisher, 3000) }
        client.initAndStart("""[{"i":30,"j":16,"type":3,"hp":99,"maxHp":99}]""", fuseMs = 50)
        publisher.await { it.isNotEmpty() }
        assertEquals(HttpStatusCode.OK, client.delete("/sessions/$key").status)
        delay(100)
        val count = publisher.events.size
        delay(500)
        assertEquals(count, publisher.events.size)
        assertEquals(HttpStatusCode.NotFound, client.postJson("/sessions/$key/auto/keepalive", "{}").status)
    }
}
