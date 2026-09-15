package com.senspark.client.explodev6

import com.senspark.game.controller.MapData
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.GameConstants
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// End-to-end: simulated heroes farming a real map against the real server code.
class ExplodeV6FlowTest {

    private class Farm(
        map: MapData,
        heroCount: Int = 1,
        bombCount: Int = 1,
        bombRange: Int = 1,
        speed: Int = 10,
        blockPass: Boolean = false,
        spawn: (Int) -> Cell = { cell(0, 0) },
    ) {
        val bed = ServerTestBed(map)
        val client = FakeGameClient(bed)
        val clock = VirtualClock()
        val targets = ClientBombTargetManager(client) { clock.nowMs() }
        val clientMap = ClientMap.mirrorOf(map)
        val heroes: List<SimulatedHero>

        init {
            // Virtual clock can't match the server's wall-clock check; PlantMoveSpeedTest covers it.
            bed.disableMoveSpeedCheck()
            heroes = (1..heroCount).map { id ->
                bed.addHero(
                    id,
                    speed = speed,
                    bombCount = bombCount,
                    bombRange = bombRange,
                    abilities = if (blockPass) setOf(GameConstants.BOMBER_ABILITY.BLOCK_PASS) else emptySet(),
                )
                SimulatedHero(
                    heroId = id,
                    speed = speed,
                    bombCount = bombCount,
                    bombRange = bombRange,
                    blockPass = blockPass,
                    currentLocation = spawn(id),
                    clientMap = clientMap,
                    targets = targets,
                    client = client,
                    clock = clock,
                )
            }
        }

        private val _spawn = spawn
        private var _newMapsSeen = 0

        val newMaps get() = _newMapsSeen

        // One frame for every hero; the clock advances once, not per hero.
        fun tick() {
            val before = clock.nowMs()
            for (hero in heroes) {
                hero.tick()
                clock.advance(before - clock.nowMs())
            }
            clock.advance((1000.0 / heroes.first().speed).toLong().coerceAtLeast(1))
            handleNewMap()
        }

        // PVE_NEW_MAP: client drops all targets, live bombs and the local map.
        private fun handleNewMap() {
            val seen = bed.pushes.count { it.command == com.senspark.game.declare.SFSCommand.PVE_NEW_MAP }
            if (seen == _newMapsSeen) return
            _newMapsSeen = seen
            targets.onNewMap()
            clientMap.bricks.clear()
            clientMap.bricks.addAll(bed.currentMap.blocks.map { cell(it.i, it.j) })
            for (hero in heroes) hero.onNewMap(freeCellNear(_spawn(hero.heroId)))
        }

        private fun freeCellNear(near: Cell): Cell {
            val seen = mutableSetOf(near)
            val queue = ArrayDeque(listOf(near))
            while (queue.isNotEmpty()) {
                val at = queue.removeFirst()
                if (clientMap.inBounds(at) && !clientMap.isWall(at) && !clientMap.hasBrick(at)) return at
                for (n in listOf(cell(at.i - 1, at.j), cell(at.i + 1, at.j), cell(at.i, at.j - 1), cell(at.i, at.j + 1))) {
                    if (!clientMap.inBounds(n) || !seen.add(n)) continue
                    queue.add(n)
                }
            }
            return near
        }

        fun run(ticks: Int) = repeat(ticks) { tick() }

        fun runUntilNewMap(maxTicks: Int): Boolean {
            repeat(maxTicks) {
                if (_newMapsSeen > 0) return true
                tick()
            }
            return _newMapsSeen > 0
        }

        // (heroId, cell, errorCode)
        val rejections
            get() = heroes.flatMap { h ->
                h.plantLog.filter { it.second != null }.map { Triple(h.heroId, it.first, it.second) }
            }

        val plantsAccepted get() = heroes.sumOf { it.plantsAccepted }
        val plantsRejected get() = heroes.sumOf { it.plantsRejected }
        val explodesAccepted get() = heroes.sumOf { it.explodesAccepted }
        val explodesRejected get() = heroes.sumOf { it.explodesRejected }
        val explodeRejections get() = heroes.flatMap { h -> h.explodeRejections.map { h.heroId to it } }
    }

    @Test
    fun `a single hero farms a map without a single rejected plant or explode`() {
        val map = TestMaps.withBricks(cell(2, 0), cell(6, 0), cell(10, 0), cell(4, 4), cell(8, 8))
        val farm = Farm(map)

        // Stop at map regeneration, which discards in-flight bombs.
        farm.runUntilNewMap(600)

        assertTrue(farm.plantsAccepted > 0, "the hero never planted anything")
        assertEquals(0, farm.plantsRejected, "rejections: ${farm.rejections}")
        assertEquals(0, farm.explodesRejected, "explode rejections: ${farm.explodeRejections}")
        assertTrue(farm.explodesAccepted > 0)
    }

    @Test
    fun `the client and server maps stay in step`() {
        val map = TestMaps.withBricks(cell(2, 0), cell(6, 0), cell(4, 4), cell(8, 8), cell(12, 2))
        val farm = Farm(map)

        farm.runUntilNewMap(800)

        val serverBricks = farm.bed.currentMap.blocks.map { cell(it.i, it.j) }.toSet()
        assertEquals(
            farm.clientMap.bricks,
            serverBricks,
            "the two maps drifted apart after ${farm.plantsAccepted} plants",
        )
    }

    @Test
    fun `a one-bomb hero re-using bomb id 0 never loses an explosion`() {
        // bombNo 0 can be in flight twice at once; every explode must still land.
        val map = TestMaps.withBricks(cell(2, 0), cell(6, 0), cell(10, 0), cell(14, 0))
        val farm = Farm(map, bombCount = 1)

        farm.runUntilNewMap(700)

        assertTrue(farm.explodesAccepted >= 3, "only ${farm.explodesAccepted} explosions landed")
        assertEquals(0, farm.explodesRejected, "a re-used bomb id lost an explosion: ${farm.explodeRejections}")
    }

    @Test
    fun `a three-bomb hero keeps all its bombs distinct`() {
        val map = TestMaps.withBricks(*(2..20 step 2).map { cell(it, 0) }.toTypedArray())
        val farm = Farm(map, bombCount = 3)

        farm.runUntilNewMap(800)

        assertTrue(farm.plantsAccepted > 3)
        assertEquals(0, farm.plantsRejected, "rejections: ${farm.rejections}")
        assertEquals(0, farm.explodesRejected, "explode rejections: ${farm.explodeRejections}")
    }

    @Test
    fun `several heroes farm the same map without colliding`() {
        val map = TestMaps.withBricks(
            *(2..30 step 4).map { cell(it, 0) }.toTypedArray(),
            *(2..30 step 4).map { cell(it, 8) }.toTypedArray(),
        )
        val farm = Farm(map, heroCount = 4, spawn = { id -> cell(0, (id - 1) * 4) })

        farm.runUntilNewMap(1000)

        assertTrue(farm.plantsAccepted > 4, "only ${farm.plantsAccepted} plants across 4 heroes")
        assertEquals(0, farm.plantsRejected, "rejections: ${farm.rejections}")
        assertEquals(0, farm.explodesRejected, "explode rejections: ${farm.explodeRejections}")
        for (hero in farm.heroes) {
            assertTrue(hero.plantsAccepted > 0, "hero ${hero.heroId} never planted -- it starved")
        }
    }

    @Test
    fun `the map is cleared and regenerated, and the heroes carry on`() {
        val map = TestMaps.withBricks(cell(2, 0), cell(6, 0))
        val farm = Farm(map)

        assertTrue(farm.runUntilNewMap(500), "the last brick was never cleared")

        val plantsBefore = farm.plantsAccepted
        farm.run(300)
        assertTrue(farm.plantsAccepted > plantsBefore, "the hero never planted again after PVE_NEW_MAP")
    }

    @Test
    fun `a hero holding no target never walks anywhere`() {
        // Without this guard the hero would walk to the default (0,0) and get stuck.
        val farm = Farm(TestMaps.empty(), spawn = { cell(10, 10) })
        val hero = farm.heroes.single()

        farm.run(200)

        assertEquals(cell(10, 10), hero.currentLocation, "the hero wandered off without a target")
        assertEquals(0, hero.plantsAccepted)
    }

    @Test
    fun `a block-pass hero sealed inside bricks still gets going`() {
        // Covers findAnyBombTarget: BFS finds nothing from inside bricks.
        val map = TestMaps.fromAscii(
            "BBBB",
            "B#B#",
            "BBBB",
        )
        val farm = Farm(map, blockPass = true, spawn = { cell(1, 0) })
        val hero = farm.heroes.single()

        farm.run(400)

        assertTrue(hero.plantsAccepted > 0, "a sealed-in block-pass hero was starved of targets")
    }

    @Test
    fun `a hero whose target loses its last brick rejects it and moves on`() {
        // Next target often loses its only brick to the bomb just planted.
        val map = TestMaps.withBricks(cell(2, 0), cell(20, 8))
        val farm = Farm(map, bombRange = 3)

        farm.runUntilNewMap(700)

        val hero = farm.heroes.single()
        assertTrue(hero.plantsAccepted > 0)
        assertEquals(0, farm.plantsRejected, "a reject must never turn into a refused plant: ${farm.rejections}")
    }

    @Test
    fun `a hero recovers from a target it can never reach`() {
        // Unreachable target: client rejects after 3s and the server picks another.
        val map = TestMaps.withBricks(
            cell(1, 0), cell(0, 1),
            cell(20, 8), cell(22, 8),
        )
        val farm = Farm(map, spawn = { cell(0, 0) })
        val hero = farm.heroes.single()

        farm.runUntilNewMap(800)

        assertTrue(hero.plantsAccepted > 0, "the hero never got unstuck")
        assertEquals(0, farm.plantsRejected, "rejections: ${farm.rejections}")
    }

    @Test
    fun `a hero that stops resolving server-side quietly stops working`() {
        // Hero disappears mid-run (deactivated, sold): no target, no plants, client not wedged.
        val map = TestMaps.withBricks(cell(2, 0), cell(6, 0), cell(10, 0), cell(14, 0))
        val farm = Farm(map)
        val hero = farm.heroes.single()

        farm.run(80)
        val plantedWhileAlive = hero.plantsAccepted
        assertTrue(plantedWhileAlive > 0)

        io.mockk.every { farm.bed.heroFiManager.getHero(1, farm.bed.dataType) } returns null
        farm.run(200)

        assertNull(farm.targets.tryGetTarget(1), "a hero the server will not target must hold none")
        assertTrue(
            hero.plantsAccepted <= plantedWhileAlive + 1,
            "it kept planting after the server stopped recognising it",
        )
    }

    @Test
    fun `a modified client cannot bomb anywhere but its assigned cell`() {
        // The V5 hole: bombing a legal cell that isn't the hero's target.
        val map = TestMaps.withBricks(*(2..24 step 2).map { cell(it, 0) }.toTypedArray())
        val farm = Farm(map)
        val hero = farm.heroes.single()

        farm.run(60)
        val held = farm.targets.tryGetTarget(1)
        assertNotNull(held, "the hero should be holding a target by now")

        val elsewhere = (0 until 30)
            .map { cell(it, 1) }
            .first {
                it != held && farm.bed.currentMap.canSetBoom(it.i, it.j) &&
                    farm.bed.currentMap.hasBlockAround(it.i, it.j)
            }

        val cheat = farm.client.startPlantBomb(1, 0, elsewhere)

        assertEquals(ErrorCode.PLANT_TARGET_MISMATCH, cheat.errorCode, "V6 accepted an off-target bomb")
        assertEquals(held, farm.bed.blockMap.debugTarget(1), "a refused plant must leave the target alone")

        val plantsBefore = hero.plantsAccepted
        farm.run(120)
        assertTrue(hero.plantsAccepted > plantsBefore)
    }
}
