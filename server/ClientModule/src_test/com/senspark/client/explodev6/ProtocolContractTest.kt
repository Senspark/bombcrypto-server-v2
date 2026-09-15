package com.senspark.client.explodev6

import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.pvp.HandlerCommand
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Wire contract with the Unity client (SFSField.cs, ErrorCode.cs, DefaultPveServerBridge.cs):
// names and types the client reads must not change.
class ProtocolContractTest {

    private lateinit var bed: ServerTestBed
    private lateinit var client: FakeGameClient

    @BeforeEach
    fun setUp() {
        bed = ServerTestBed(TestMaps.withBricks(cell(6, 0), cell(10, 4)))
        bed.disableMoveSpeedCheck()
        client = FakeGameClient(bed)
        bed.addHero(1)
    }

    private fun raw(
        handler: BaseEncryptRequestHandler,
        build: ISFSObject.() -> Unit,
    ): ServerTestBed.ServerResponse = bed.dispatch(handler, SFSObject().apply(build))

    @Test
    fun `the client-facing commands are named exactly as the client expects`() {
        assertEquals("GET_BOMB_TARGET", SFSCommand.GET_BOMB_TARGET)
        assertEquals("START_PLANT_BOMB", SFSCommand.START_PLANT_BOMB)
        // RESPONSE_EXPLODE is a push, so it lives in HandlerCommand.
        assertEquals("RESPONSE_EXPLODE", HandlerCommand.ResponseExplode)
    }

    @Test
    fun `the error codes are the ones the client branches on`() {
        assertEquals(1063, ErrorCode.PLANT_TARGET_MISMATCH)
        assertEquals(1064, ErrorCode.EXPLODE_POSITION_MISMATCH)
        assertEquals(1065, ErrorCode.NO_BOMB_TARGET)
        assertEquals(1066, ErrorCode.PLANT_TOO_FAST)
        assertEquals(1002, ErrorCode.BOMBERMAN_NULL)
        assertEquals(1010, ErrorCode.BOMBERMAN_ACTIVE_INVALID)
        assertEquals(1005, ErrorCode.BOMBERMAN_IS_NOT_WORKING)
    }

    @Test
    fun `the field names are the ones the client reads`() {
        assertEquals("targets", SFSField.Targets)
        assertEquals("hero_type", SFSField.HeroType)
        assertEquals("id", SFSField.ID)
        assertEquals("blocks", SFSField.Blocks)
        assertEquals("energy", SFSField.Energy)
    }

    @Test
    fun `GET_BOMB_TARGET answers targets entries of id, hero_type, i, j`() {
        val response = raw(bed.getBombTargetHandler) {
            val heroes = SFSArray()
            heroes.addSFSObject(SFSObject().apply {
                putLong("id", 1L)
                putInt("i", 0)
                putInt("j", 0)
            })
            putSFSArray("heroes", heroes)
        }

        val targets = response.require().getSFSArray("targets")
        assertNotNull(targets)
        assertEquals(1, targets.size())
        val entry = targets.getSFSObject(0)
        // Client reads id as long, the rest as int.
        assertEquals(1L, entry.getLong("id"))
        assertNotNull(entry.getInt("hero_type"))
        assertNotNull(entry.getInt("i"))
        assertNotNull(entry.getInt("j"))
    }

    @Test
    fun `an empty heroes array is answered with an empty targets array, not an error`() {
        val response = raw(bed.getBombTargetHandler) {
            putSFSArray("heroes", SFSArray())
        }
        assertTrue(!response.isError)
        assertEquals(0, response.require().getSFSArray("targets").size())
    }

    @Test
    fun `START_PLANT_BOMB answers the next target in the same shape`() {
        val target = client.getBombTarget(1, cell(0, 0))
        assertNotNull(target)

        val response = raw(bed.startPlantBombHandler) {
            putLong("id", 1L)
            putInt("num", 0)
            putInt("i", target.i)
            putInt("j", target.j)
        }

        val body = response.require()
        assertEquals(1L, body.getLong("id"))
        assertNotNull(body.getInt("hero_type"))
        assertNotNull(body.getInt("i"))
        assertNotNull(body.getInt("j"))
    }

    @Test
    fun `RESPONSE_EXPLODE is pushed with num, i, j plus the V5 body the client already parses`() {
        // Push has no rid, so num/i/j identify which bomb exploded.
        val target = client.getBombTarget(1, cell(0, 0))
        assertNotNull(target)
        assertTrue(client.startPlantBomb(1, 0, target).isOk)

        val before = bed.pushes.size
        assertTrue(bed.fireFuse(1, 0, target.i, target.j))
        val push = bed.pushes.drop(before).single { it.command == HandlerCommand.ResponseExplode }

        val body = push.data
        assertEquals(1L, body.getLong("id"))
        assertEquals(0, body.getInt("num"))
        assertEquals(target.i, body.getInt("i"))
        assertEquals(target.j, body.getInt("j"))
        assertNotNull(body.getInt("energy"))
        assertNotNull(body.getSFSArray("blocks"))
        assertNotNull(body.getIntArray("attend_pools"))
    }

    @Test
    fun `an error response carries ec and es, and no encrypted body`() {
        val response = raw(bed.startPlantBombHandler) {
            putLong("id", 1L)
            putInt("num", 0)
            putInt("i", 5)
            putInt("j", 0)
        }
        assertEquals(ErrorCode.PLANT_TARGET_MISMATCH, response.errorCode)
        assertEquals(null, response.data)
    }

    @Test
    fun `hero ids survive the JSON round trip as longs`() {
        // Client sends id as long, handlers read int; works only within Int range.
        val bigId = 2_000_000_000
        bed.addHero(bigId)
        val target = client.getBombTarget(bigId, cell(0, 0))
        assertNotNull(target)

        val plant = client.startPlantBomb(bigId, 0, target)
        assertTrue(plant.isOk, "error ${plant.errorCode}")
        assertEquals(bigId, plant.nextTarget?.heroId)
    }

    @Test
    fun `the reject fields are optional`() {
        val withoutReject = client.getBombTargets(mapOf(1 to cell(0, 0)))
        assertEquals(1, withoutReject.size)

        val withReject = client.getBombTargets(mapOf(1 to cell(0, 0)), mapOf(1 to withoutReject[0].location))
        assertEquals(1, withReject.size)
    }
}
