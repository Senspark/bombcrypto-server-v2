package com.senspark.client.mapservicefuse

import com.senspark.game.declare.EnumConstants.SAVE
import com.senspark.game.declare.GameConstants
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.manager.blockMap.mapservice.MsBlockHitDto
import com.senspark.game.manager.blockMap.mapservice.MsExplodeResultEvent
import com.senspark.game.manager.blockMap.mapservice.MsPlantResponse
import com.senspark.game.manager.blockMap.mapservice.MsRewardHitDto
import com.senspark.game.pvp.HandlerCommand
import com.senspark.game.utils.serialize
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Plant sends hero snapshot + fuse to MapService; the stream result becomes RESPONSE_EXPLODE.
class MapServiceFuseV2Test {
    private val fake = FakeMapServiceClient()
    private val bed = MapServiceFuseTestBed(MapServiceFuseTestBed.mapOf(1 to 0, 5 to 0), fake)

    private fun event(
        takeResult: String = "OK",
        blocksHit: List<MsBlockHitDto> = listOf(MsBlockHitDto(1, 0, 0, GameConstants.BLOCK_TYPE.WOODEN, listOf(MsRewardHitDto("COIN", 1.5f)))),
        mapNowEmpty: Boolean = false,
        sessionKey: String = bed.sessionKey,
    ) = MsExplodeResultEvent(
        sessionKey = sessionKey, heroId = 1, bombNo = 0, i = 0, j = 0, takeResult = takeResult,
        blocksHit = blocksHit, mapNowEmpty = mapNowEmpty, plantedAtMs = System.currentTimeMillis() - 3000,
        explodedAtMs = System.currentTimeMillis(),
    ).serialize()

    private fun plant() {
        assertNotNull(bed.plantViaHandler(heroId = 1, bombNo = 0, i = 0, j = 0), "plant must succeed")
    }

    @Test
    fun `plant sends the hero snapshot and fuse to MapService, schedules nothing locally, and answers as before`() {
        bed.addHero(1)
        val response = bed.plantViaHandler(heroId = 1, bombNo = 0, i = 0, j = 0)

        assertNotNull(response)
        assertEquals(1L, response.getLong(SFSField.ID))
        assertEquals(2 to 0, response.getInt("i") to response.getInt("j"), "the next target rides the plant response as before")

        val sent = fake.plants.single()
        assertEquals(3000L, sent.fuseMs)
        val hero = assertNotNull(sent.hero)
        assertEquals(1, hero.heroId)
        assertEquals(2, hero.bombRange)
        assertEquals(7, hero.damageTreasure)
        assertEquals(1, hero.totalPower)
        assertEquals(bed.dataType.name, hero.dataType)

        verify(exactly = 0) { bed.scheduler.scheduleOnce(any(), any(), any()) }
        assertEquals(0, fake.explodeCalls, "the server never calls /explode any more")
        assertTrue(bed.pushesOf(HandlerCommand.ResponseExplode).isEmpty(), "nothing explodes until MapService says so")
    }

    @Test
    fun `an explode result from the stream becomes the RESPONSE_EXPLODE push`() {
        val hero = bed.addHero(1)
        plant()

        assertTrue(bed.router.handle(event()), "owned session: the stream entry is consumed")

        val push = bed.pushesOf(HandlerCommand.ResponseExplode).single().data
        assertEquals(1L, push.getLong(SFSField.ID))
        assertEquals(0, push.getInt("num"))
        assertEquals(0 to 0, push.getInt("i") to push.getInt("j"))
        val block = push.getSFSArray(SFSField.Blocks).getSFSObject(0)
        assertEquals(1 to 0, block.getInt("i") to block.getInt("j"))
        assertEquals(0, block.getInt(SFSField.HP))
        val reward = block.getSFSArray(SFSField.Rewards).getSFSObject(0)
        assertEquals("COIN", reward.getUtfString(SFSField.Type))
        assertEquals(1.5f, reward.getFloat(SFSField.Value))

        verify(exactly = 1) { hero.subEnergy() }
        verify(exactly = 1) { bed.blockRewardManager.addRewards(any()) }
        assertNull(currentMap().getBlockMap(1, 0), "local map mirrors MapService's blast")
        assertTrue(bed.saves.containsAll(listOf(SAVE.MAP, SAVE.REWARD, SAVE.HERO_STATUS)))
    }

    @Test
    fun `a non-OK take result pushes nothing`() {
        val hero = bed.addHero(1)
        plant()

        assertTrue(bed.router.handle(event(takeResult = "ALREADY_TAKEN", blocksHit = emptyList())))
        assertTrue(bed.pushesOf(HandlerCommand.ResponseExplode).isEmpty())
        verify(exactly = 0) { hero.subEnergy() }
    }

    @Test
    fun `a hero that stopped working gets no push and no energy change, but the map still mirrors MapService`() {
        val hero = bed.addHero(1)
        plant()
        io.mockk.every { hero.stage } returns GameConstants.BOMBER_STAGE.SLEEP

        bed.router.handle(event())

        assertTrue(bed.pushesOf(HandlerCommand.ResponseExplode).isEmpty())
        verify(exactly = 0) { hero.subEnergy() }
        verify(exactly = 0) { bed.blockRewardManager.addRewards(any()) }
        assertNull(currentMap().getBlockMap(1, 0))
    }

    @Test
    fun `a hero out of energy gets the empty-blocks push, as before`() {
        val hero = bed.addHero(1)
        plant()
        io.mockk.every { hero.energy } returns 0

        bed.router.handle(event())

        val push = bed.pushesOf(HandlerCommand.ResponseExplode).single().data
        assertEquals(0, push.getSFSArray(SFSField.Blocks).size())
        assertEquals(0, push.getInt(SFSField.Energy))
        verify(exactly = 0) { hero.subEnergy() }
    }

    @Test
    fun `a map-clearing result regenerates the map, pushes it to MapService, then pushes the explode`() {
        bed.addHero(1)
        plant()

        bed.router.handle(event(mapNowEmpty = true))

        assertEquals(1, fake.replacedMaps.size)
        val commands = synchronized(bed.pushes) { bed.pushes.map { it.command } }
        assertEquals(listOf(SFSCommand.PVE_NEW_MAP, HandlerCommand.ResponseExplode), commands)
    }

    @Test
    fun `entries for sessions this server does not own are left on the stream`() {
        bed.addHero(1)
        assertFalse(bed.router.handle(event()), "never planted here: not registered")

        plant()
        assertFalse(bed.router.handle(event(sessionKey = "999-BSC-PVE_V2")))

        bed.blockMap.notifySessionEnd()
        assertFalse(bed.router.handle(event()), "logout unregisters the session")
        assertTrue(bed.pushesOf(HandlerCommand.ResponseExplode).isEmpty())
    }

    @Test
    fun `a malformed entry is consumed without touching anything`() {
        bed.addHero(1)
        plant()
        assertTrue(bed.router.handle("{not json"))
        assertTrue(bed.pushesOf(HandlerCommand.ResponseExplode).isEmpty())
    }

    @Test
    fun `an OK plant with no fuse armed (outdated MapService) is logged loudly`() {
        bed.addHero(1)
        fake.plantResponse = MsPlantResponse(result = "OK", nextTarget = null, fuseArmed = false)
        bed.plantViaHandler(heroId = 1, bombNo = 0, i = 0, j = 0)
        assertTrue(bed.loggedErrors.any { it.contains("armed no fuse") }, bed.loggedErrors.toString())
    }

    private fun currentMap(): com.senspark.game.controller.MapData {
        val getter = bed.blockMap.javaClass.getDeclaredMethod("get_mapData")
        getter.isAccessible = true
        return getter.invoke(bed.blockMap) as com.senspark.game.controller.MapData
    }
}
