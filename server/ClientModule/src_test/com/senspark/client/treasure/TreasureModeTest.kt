package com.senspark.client.treasure

import com.senspark.game.declare.GameConstants
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.manager.blockMap.mapservice.MapServiceSessionNotFoundException
import com.senspark.game.manager.blockMap.mapservice.MsBlockHitDto
import com.senspark.game.manager.blockMap.mapservice.MsCellDto
import com.senspark.game.manager.blockMap.mapservice.MsHeroPositionDto
import com.senspark.game.manager.blockMap.mapservice.MsRewardHitDto
import com.senspark.game.manager.blockMap.mapservice.MsTreasureEventBatch
import com.senspark.game.manager.blockMap.mapservice.MsTreasureEventDto
import com.senspark.game.pvp.HandlerCommand
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import com.senspark.game.utils.serialize
import com.smartfoxserver.v2.entities.data.ISFSObject
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Server-driven treasure mode through the real handler + UserBlockMapManagerV2 + router; MapService is faked.
class TreasureModeTest {
    private val fake = FakeMapServiceClient()
    private val bed = TreasureTestBed(TreasureTestBed.mapOf(1 to 0, 5 to 0), fake)

    private fun batch(vararg events: MsTreasureEventDto, sessionKey: String = bed.sessionKey) =
        MsTreasureEventBatch(sessionKey, events.toList()).serialize()

    private fun move(seq: Long, heroId: Int = 1) = MsTreasureEventDto(
        seq, "MOVE", 1_000 + seq, heroId, 0, 0, path = listOf(MsCellDto(0, 1), MsCellDto(0, 2)), stepMs = 100,
    )

    private fun explode(seq: Long, heroId: Int = 1, mapNowEmpty: Boolean = false, hits: List<MsBlockHitDto> = listOf(
        MsBlockHitDto(1, 0, 0, GameConstants.BLOCK_TYPE.WOODEN, listOf(MsRewardHitDto("COIN", 1.5f))),
    )) = MsTreasureEventDto(
        seq, "EXPLODE", 4_000, heroId, 0, 0, bombNo = 0, plantedAtMs = 1_000, explodeAtMs = 4_000,
        takeResult = "OK", blocksHit = hits, mapNowEmpty = mapNowEmpty,
    )

    private fun events(): List<ISFSObject> = bed.pushesOf(HandlerCommand.TreasureEvents).flatMap { push ->
        val arr = push.data.getSFSArray("events")
        (0 until arr.size()).map { arr.getSFSObject(it) }
    }

    // Roster sync and keepalive run on Dispatchers.IO.
    private fun eventually(what: String, check: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 3000
        while (!check()) {
            if (System.currentTimeMillis() > deadline) throw AssertionError("timed out waiting for $what")
            Thread.sleep(10)
        }
    }

    private fun start(): ISFSObject = assertNotNull(bed.startTreasureViaHandler(), "START_TREASURE_MODE failed: ${bed.loggedErrors}")

    @Test
    fun `start plays only working heroes with energy and answers with map, positions and seq`() {
        bed.addHero(1)
        bed.addHero(2, stage = GameConstants.BOMBER_STAGE.SLEEP)
        bed.addHero(3, energy = 0)
        bed.addHero(4, active = false)
        val hero5 = bed.addHero(5)
        every { hero5.containsAbility(GameConstants.BOMBER_ABILITY.BLOCK_PASS) } returns true

        val response = start()

        val request = fake.autoStarts.single()
        assertEquals(listOf(1, 5), request.heroes.map { it.hero.heroId })
        assertEquals(3000L, request.fuseMs, "fuse comes from time_bomb_explode")
        assertEquals(10, request.heroes[0].speed)
        assertEquals(1, request.heroes[0].bombCount)
        assertEquals(listOf(false, true), request.heroes.map { it.blockPass })

        assertTrue(response.getUtfString("datas_pve_v2").contains("\"i\":1"))
        assertEquals(0L, response.getLong("seq"))
        assertEquals(3000L, response.getLong("fuse_ms"))
        val heroes = response.getSFSArray("heroes")
        assertEquals(listOf(1L, 5L), (0 until heroes.size()).map { heroes.getSFSObject(it).getLong(SFSField.ID) })
        assertNotNull(response.getSFSArray("dangerous"))
    }

    @Test
    fun `move and plant events are forwarded in one ordered push`() {
        bed.addHero(1)
        start()
        val plant = MsTreasureEventDto(3, "PLANT", 1_200, 1, 0, 2, bombNo = 7, plantedAtMs = 1_200, explodeAtMs = 4_200)
        assertTrue(bed.treasureRouter.handle(batch(move(2), plant)))

        val (m, p) = events()
        assertEquals("MOVE", m.getUtfString("type"))
        assertEquals(2L, m.getLong("seq"))
        assertEquals(100L, m.getLong("step_ms"))
        val path = m.getSFSArray("path")
        assertEquals(listOf(0 to 1, 0 to 2), (0 until path.size()).map { path.getSFSObject(it).let { c -> c.getInt("i") to c.getInt("j") } })
        assertEquals("PLANT", p.getUtfString("type"))
        assertEquals(7, p.getInt("num"))
        assertEquals(4_200L, p.getLong("explode_at"))
        assertEquals(1, bed.pushesOf(HandlerCommand.TreasureEvents).size, "one batch, one push")
    }

    @Test
    fun `explode credits energy and rewards and mirrors the map`() {
        val hero = bed.addHero(1)
        start()
        bed.treasureRouter.handle(batch(explode(5)))

        verify { hero.subEnergy() }
        verify { bed.blockRewardManager.addRewards(any()) }
        val e = events().single()
        assertEquals("EXPLODE", e.getUtfString("type"))
        val block = e.getSFSArray(SFSField.Blocks).getSFSObject(0)
        assertEquals(0, block.getInt(SFSField.HP))
        assertEquals("COIN", block.getSFSArray(SFSField.Rewards).getSFSObject(0).getUtfString(SFSField.Type))
        assertEquals(null, bed.map.getBlockMap(1, 0), "destroyed block removed from the server's map")
    }

    @Test
    fun `a hero that stopped working still shows its blast but earns nothing and leaves the game`() {
        val hero = bed.addHero(1)
        start()
        every { hero.stage } returns GameConstants.BOMBER_STAGE.SLEEP
        bed.treasureRouter.handle(batch(explode(5)))

        verify(exactly = 0) { bed.blockRewardManager.addRewards(any()) }
        val block = events().single().getSFSArray(SFSField.Blocks).getSFSObject(0)
        assertEquals(0, block.getInt(SFSField.HP))
        assertFalse(block.containsKey(SFSField.Rewards))
        eventually("roster removal") { fake.autoHeroEdits.isNotEmpty() }
        val edit = fake.autoHeroEdits.single()
        assertEquals(listOf(1), edit.remove)
        assertEquals("not_working", edit.reason)
    }

    @Test
    fun `running out of energy removes the hero`() {
        val hero = bed.addHero(1)
        bed.addHero(2)
        start()
        every { hero.energy } returns 0
        bed.treasureRouter.handle(batch(explode(5)))
        eventually("roster removal") { fake.autoHeroEdits.isNotEmpty() }
        assertEquals("no_energy", fake.autoHeroEdits.single().reason)
        assertEquals(listOf(1), fake.autoHeroEdits.single().remove)
    }

    @Test
    fun `a hero sent to work joins, one sent to sleep leaves`() {
        bed.addHero(1)
        val sleeper = bed.addHero(2, stage = GameConstants.BOMBER_STAGE.SLEEP)
        start()
        every { sleeper.stage } returns GameConstants.BOMBER_STAGE.WORK
        bed.blockMap.syncTreasureHeroes()
        eventually("join") { fake.autoHeroEdits.size == 1 }
        assertEquals(listOf(2), fake.autoHeroEdits.last().upsert.map { it.hero.heroId })

        every { sleeper.stage } returns GameConstants.BOMBER_STAGE.HOUSE
        bed.blockMap.syncTreasureHeroes()
        eventually("leave") { fake.autoHeroEdits.size == 2 }
        assertEquals(listOf(2), fake.autoHeroEdits.last().remove)
        bed.blockMap.syncTreasureHeroes()
        Thread.sleep(200)
        assertEquals(2, fake.autoHeroEdits.size, "nothing changed, nothing sent")
    }

    @Test
    fun `the map clearing makes a new map for MapService, announced by its NEW_MAP event, not PVE_NEW_MAP`() {
        bed.addHero(1)
        start()
        bed.treasureRouter.handle(batch(explode(5, mapNowEmpty = true)))
        assertEquals(1, fake.replacedMaps.size)
        assertTrue(bed.pushesOf(SFSCommand.PVE_NEW_MAP).isEmpty())

        val newMap = MsTreasureEventDto(6, "NEW_MAP", 4_000, heroes = listOf(MsHeroPositionDto(1, 4, 4)), resumeAtMs = 7_000)
        bed.treasureRouter.handle(batch(newMap))
        val e = events().last()
        assertEquals("NEW_MAP", e.getUtfString("type"))
        val sentBlocks = fake.replacedMaps.single().blocks
        assertEquals(sentBlocks.size, Json.parseToJsonElement(e.getUtfString("datas_pve_v2")).jsonArray.size)
        assertEquals(7_000L, e.getLong("resume_at"))
        assertEquals(4 to 4, e.getSFSArray("heroes").getSFSObject(0).let { it.getInt("i") to it.getInt("j") })
    }

    @Test
    fun `keepalive re-creates a lost MapService game and pushes a RESYNC snapshot`() {
        bed.addHero(1)
        val tick = slot<() -> Unit>()
        every { bed.scheduler.schedule(any(), any(), any(), capture(tick)) } answers { }
        start()

        fake.keepaliveError = MapServiceSessionNotFoundException()
        fake.autoSeq = 0
        tick.captured.invoke()

        eventually("resync") { bed.pushesOf(HandlerCommand.TreasureEvents).isNotEmpty() }
        assertEquals(2, fake.autoStarts.size)
        val resync = events().single()
        assertEquals("RESYNC", resync.getUtfString("type"))
        assertEquals("map_service_lost", resync.getUtfString("reason"))
        assertTrue(resync.containsKey("datas_pve_v2"))
    }

    @Test
    fun `stop halts the game but late explodes are still credited`() {
        val hero = bed.addHero(1)
        start()
        bed.treasureRouter.handle(batch(move(1)))
        assertNotNull(bed.callHandler(com.senspark.game.handler.airdropUser.StopTreasureModeHandler(), com.smartfoxserver.v2.entities.data.SFSObject()))
        assertEquals(1, fake.autoStops)

        bed.treasureRouter.handle(batch(explode(2)))
        verify { hero.subEnergy() }
        bed.blockMap.syncTreasureHeroes()
        Thread.sleep(200)
        assertTrue(fake.autoHeroEdits.isEmpty(), "stopped: roster no longer synced")
    }

    private fun pause(paused: Boolean) = assertNotNull(
        bed.callHandler(
            if (paused) com.senspark.game.handler.airdropUser.PauseTreasureModeHandler() else com.senspark.game.handler.airdropUser.ResumeTreasureModeHandler(),
            com.smartfoxserver.v2.entities.data.SFSObject(),
        )
    )

    @Test
    fun `pause and resume reach MapService, a paused START keeps the heroes still, stop clears the pause`() {
        bed.addHero(1)
        start()
        pause(true)
        pause(false)
        assertEquals(listOf(true, false), fake.autoPauses)

        val request = com.smartfoxserver.v2.entities.data.SFSObject().apply { putBool("paused", true) }
        assertNotNull(bed.callHandler(bed.startTreasureModeHandler, request))
        assertTrue(fake.autoStarts.last().paused, "resync from a paused client stays paused")

        assertNotNull(bed.callHandler(com.senspark.game.handler.airdropUser.StopTreasureModeHandler(), com.smartfoxserver.v2.entities.data.SFSObject()))
        pause(true)
        assertEquals(listOf(true, false), fake.autoPauses, "no game running: nothing sent")
        start()
        assertFalse(fake.autoStarts.last().paused)
    }

    @Test
    fun `a pause MapService lost restarts the game paused, keepalive re-sends a pause it missed`() {
        bed.addHero(1)
        val tick = slot<() -> Unit>()
        every { bed.scheduler.schedule(any(), any(), any(), capture(tick)) } answers { }
        start()

        fake.autoRunning = false
        pause(true)
        assertEquals(2, fake.autoStarts.size)
        assertTrue(fake.autoStarts.last().paused)
        assertEquals("RESYNC", events().single().getUtfString("type"))

        fake.autoPaused = false
        tick.captured.invoke()
        eventually("pause re-sent") { fake.autoPauses.lastOrNull() == true }
    }

    @Test
    fun `batches for other sessions are left on the stream, logout unregisters`() {
        bed.addHero(1)
        start()
        assertFalse(bed.treasureRouter.handle(batch(move(1), sessionKey = "999-BSC-PVE_V2")))
        bed.blockMap.notifySessionEnd()
        assertFalse(bed.treasureRouter.handle(batch(move(2))))
    }

    @Test
    fun `decodes the compact JSON MapService publishes`() {
        // Exactly what map-service's RedisStreamTreasureEventPublisher writes (nulls omitted).
        val raw = """{"sessionKey":"${bed.sessionKey}","events":[
            {"seq":1,"type":"MOVE","atMs":10,"heroId":1,"i":0,"j":0,"path":[{"i":1,"j":0}],"stepMs":200},
            {"seq":2,"type":"PLANT","atMs":210,"heroId":1,"i":1,"j":0,"bombNo":0,"plantedAtMs":210,"explodeAtMs":3210},
            {"seq":3,"type":"HERO_LEAVE","atMs":300,"heroId":1,"i":1,"j":0,"reason":"energy"}]}"""
        bed.addHero(1)
        start()
        assertTrue(bed.treasureRouter.handle(raw))
        assertEquals(listOf("MOVE", "PLANT", "HERO_LEAVE"), events().map { it.getUtfString("type") })
        assertEquals("energy", events().last().getUtfString("reason"))
    }
}
