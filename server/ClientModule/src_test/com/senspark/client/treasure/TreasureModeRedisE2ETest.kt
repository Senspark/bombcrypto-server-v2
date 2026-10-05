package com.senspark.client.treasure

import com.senspark.common.cache.RedisServices
import com.senspark.common.service.SimpleScheduler
import com.senspark.game.constant.StreamKeys
import com.senspark.game.declare.SFSField
import com.senspark.game.manager.blockMap.mapservice.MapServiceClient
import com.senspark.game.manager.blockMap.mapservice.MapTreasureEventRouter
import com.senspark.game.pvp.HandlerCommand
import com.smartfoxserver.v2.entities.data.ISFSObject
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assumptions.assumeTrue
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Real MapService + Redis. Opt-in: set MAP_SERVICE_E2E_URL (MapService must use the same REDIS_CONNECTION_STRING).
class TreasureModeRedisE2ETest {
    @Test
    fun `START_TREASURE_MODE makes MapService play the hero and every step reaches the client in order`() {
        val mapServiceUrl = System.getenv("MAP_SERVICE_E2E_URL")
        assumeTrue(!mapServiceUrl.isNullOrBlank(), "MAP_SERVICE_E2E_URL not set")
        val redisUrl = System.getenv("REDIS_CONNECTION_STRING") ?: "redis://localhost:6379"

        val fastStreamRedis = RedisServices.createFastStreamRedis(redisUrl, SimpleScheduler(), mockk(relaxed = true))
        val router = MapTreasureEventRouter(mockk(relaxed = true))
        fastStreamRedis.listen(StreamKeys.AP_MAP_TREASURE_EVENT_STR) { router.handle(it.value) }
        fastStreamRedis.initialize()
        try {
            val bed = TreasureTestBed(
                map = TreasureTestBed.mapOf(3 to 0, 6 to 2, hp = 1),
                mapService = MapServiceClient(mapServiceUrl),
                userId = Random.nextInt(900_000_000, Int.MAX_VALUE),
                treasureRouter = router,
            )
            every { bed.gameConfig.timeBombExplode } returns 500
            bed.addHero(1)
            val started = System.currentTimeMillis()
            val response = assertNotNull(bed.startTreasureViaHandler(), "start failed: ${bed.loggedErrors}")
            assertEquals(1, response.getSFSArray("heroes").size())

            fun events(): List<ISFSObject> = bed.pushesOf(HandlerCommand.TreasureEvents).flatMap { push ->
                val arr = push.data.getSFSArray("events")
                (0 until arr.size()).map { arr.getSFSObject(it) }
            }
            val deadline = started + 10_000
            while (events().none { it.getUtfString("type") == "NEW_MAP" } && System.currentTimeMillis() < deadline) {
                Thread.sleep(20)
            }
            val all = events()
            println("[E2E] ${all.size} events in ${System.currentTimeMillis() - started}ms: ${all.map { it.getUtfString("type") }}")

            val types = all.map { it.getUtfString("type") }
            assertTrue("PLANT" in types && "EXPLODE" in types, "no plant/explode: $types, logged: ${bed.loggedErrors}")
            assertEquals(all.map { it.getLong("seq") }, (1L..all.size).toList(), "seq arrives complete and in order")
            val firstExplode = all.first { it.getUtfString("type") == "EXPLODE" }
            assertTrue(firstExplode.getSFSArray(SFSField.Blocks).size() > 0, "the brick next to the bomb is hit")
            assertTrue("NEW_MAP" in types, "clearing both bricks makes a new map: $types")
            assertEquals(1, bed.pushesOf(HandlerCommand.TreasureEvents).count { push ->
                val arr = push.data.getSFSArray("events")
                (0 until arr.size()).any { arr.getSFSObject(it).getUtfString("type") == "NEW_MAP" }
            })

            bed.blockMap.stopTreasureMode()
            bed.blockMap.notifySessionEnd()
        } finally {
            fastStreamRedis.destroy()
        }
    }
}
