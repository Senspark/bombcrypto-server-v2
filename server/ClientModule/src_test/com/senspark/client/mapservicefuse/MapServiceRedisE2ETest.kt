package com.senspark.client.mapservicefuse

import com.senspark.common.cache.RedisServices
import com.senspark.common.service.SimpleScheduler
import com.senspark.game.constant.StreamKeys
import com.senspark.game.declare.SFSField
import com.senspark.game.manager.blockMap.HeroTargetRequest
import com.senspark.game.manager.blockMap.mapservice.MapExplodeResultRouter
import com.senspark.game.manager.blockMap.mapservice.MapServiceClient
import com.senspark.game.pvp.HandlerCommand
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assumptions.assumeTrue
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Real MapService + Redis end-to-end. Opt-in: set MAP_SERVICE_E2E_URL (MapService must use the
// same REDIS_CONNECTION_STRING, default redis://localhost:6379).
class MapServiceRedisE2ETest {
    @Test
    fun `a bomb planted on MapService explodes after its fuse and reaches the client as RESPONSE_EXPLODE`() {
        val mapServiceUrl = System.getenv("MAP_SERVICE_E2E_URL")
        assumeTrue(!mapServiceUrl.isNullOrBlank(), "MAP_SERVICE_E2E_URL not set")
        val redisUrl = System.getenv("REDIS_CONNECTION_STRING") ?: "redis://localhost:6379"

        val fastStreamRedis = RedisServices.createFastStreamRedis(redisUrl, SimpleScheduler(), mockk(relaxed = true))
        val router = MapExplodeResultRouter(mockk(relaxed = true))
        fastStreamRedis.listen(StreamKeys.AP_MAP_EXPLODE_RESULT_STR) { router.handle(it.value) }
        fastStreamRedis.initialize()
        try {
            // Random user id avoids clashing with real sessions.
            val bed = MapServiceFuseTestBed(
                map = MapServiceFuseTestBed.mapOf(1 to 0, 6 to 0),
                mapService = MapServiceClient(mapServiceUrl),
                userId = Random.nextInt(900_000_000, Int.MAX_VALUE),
                router = router,
            )
            val fuseMs = 1000
            every { bed.gameConfig.timeBombExplode } returns fuseMs
            bed.addHero(1)
            bed.blockMap.getBlockMap() // inits the MapService session

            val target = bed.blockMap.getOrCreateTargets(listOf(HeroTargetRequest(1, 0 to 0, null))).single().target
            val plantedAt = System.currentTimeMillis()
            assertNotNull(bed.plantViaHandler(heroId = 1, bombNo = 0, i = target.first, j = target.second), "plant failed: ${bed.loggedErrors}")

            val deadline = plantedAt + fuseMs + 3000
            while (bed.pushesOf(HandlerCommand.ResponseExplode).isEmpty() && System.currentTimeMillis() < deadline) {
                Thread.sleep(20)
            }
            val arrivedAfterMs = System.currentTimeMillis() - plantedAt
            val push = bed.pushesOf(HandlerCommand.ResponseExplode).singleOrNull()?.data
            assertNotNull(push, "no RESPONSE_EXPLODE within ${fuseMs + 3000}ms; logged: ${bed.loggedErrors}")
            println("[E2E] RESPONSE_EXPLODE arrived ${arrivedAfterMs}ms after plant (fuse ${fuseMs}ms)")

            assertTrue(arrivedAfterMs >= fuseMs, "exploded before the fuse elapsed (${arrivedAfterMs}ms)")
            assertTrue(arrivedAfterMs < fuseMs + 1000, "stream delivery too slow (${arrivedAfterMs}ms)")
            assertEquals(target, push.getInt("i") to push.getInt("j"))
            assertEquals(0, push.getInt("num"))
            assertTrue(push.getSFSArray(SFSField.Blocks).size() > 0, "the brick next to the target must be hit")

            bed.blockMap.notifySessionEnd()
        } finally {
            fastStreamRedis.destroy()
        }
    }
}
