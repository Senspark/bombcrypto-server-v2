package com.senspark.mapservice.auto

import com.senspark.mapservice.domain.GameConstants
import com.senspark.mapservice.domain.MapGrid
import com.senspark.mapservice.domain.RewardConfig
import com.senspark.mapservice.domain.RewardEntry
import com.senspark.mapservice.domain.Session
import com.senspark.mapservice.domain.SessionConfig
import com.senspark.mapservice.model.AutoHeroDto
import com.senspark.mapservice.model.AutoSnapshotDto
import com.senspark.mapservice.model.HeroPositionDto
import com.senspark.mapservice.model.BlockDto
import com.senspark.mapservice.model.CellDto
import com.senspark.mapservice.model.TreasureEventDto
import com.senspark.mapservice.model.TreasureEventType
import com.senspark.mapservice.stresstest.TreasureReplay
import com.senspark.mapservice.testHero
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutoPlayTest {
    private fun block(i: Int, j: Int, hp: Int = 1, type: Int = GameConstants.BlockType.WOODEN) = BlockDto(i, j, type, hp, hp)

    private fun session(vararg blocks: BlockDto, reward: RewardConfig = RewardConfig()) =
        Session(MapGrid.fromDtos(blocks.toList(), 0, "PVE_V2"), SessionConfig(reward = reward))

    private fun hero(id: Int, speed: Int = 5, bombs: Int = 1, blockPass: Boolean = false) =
        AutoHeroDto(testHero(id), speed, bombs, blockPass)

    private class Bed(val session: Session, spawns: List<Cell>, fuseMs: Long = 3000, pauseMs: Long = 3000) {
        val auto = AutoPlay(session, AutoConfig(fuseMs, pauseMs), Random(1)).also {
            session.autoPlay = it
            val queue = ArrayDeque(spawns)
            it.spawnOverride = { n -> List(n) { queue.removeFirst() } }
        }
        val events = mutableListOf<TreasureEventDto>()

        fun start(heroes: List<AutoHeroDto>, now: Long = 0) = auto.start(heroes, auto.config, now)

        // advance() runs every due action at its own scheduled time, however late it's called.
        fun runUntil(t: Long): List<TreasureEventDto> {
            auto.advance(t)
            return auto.drainEvents().also { events.addAll(it) }
        }

        fun of(type: String) = events.filter { it.type == type }

        // MOVEs that walk somewhere; empty-path ones are stand / roam notices.
        fun walks() = of(TreasureEventType.MOVE).filter { !it.path.isNullOrEmpty() }
    }

    @Test
    fun `hero walks the client's BFS path one tile per 1000 over speed ms, plants on arrival, bomb explodes after the fuse`() {
        // Brick at (4,0); nearest brick-adjacent cell from (0,0) is (3,0).
        val bed = Bed(session(block(4, 0)), spawns = listOf(0 to 0))
        bed.start(listOf(hero(1, speed = 5)))
        bed.runUntil(10_000)

        val move = bed.of(TreasureEventType.MOVE).first()
        assertEquals(0L, move.atMs)
        assertEquals(0 to 0, move.i to move.j)
        assertEquals(listOf(CellDto(1, 0), CellDto(2, 0), CellDto(3, 0)), move.path)
        assertEquals(200L, move.stepMs)

        val plant = bed.of(TreasureEventType.PLANT).first()
        assertEquals(600L, plant.atMs, "3 tiles at 200ms each")
        assertEquals(3 to 0, plant.i to plant.j)
        assertEquals(3600L, plant.explodeAtMs)

        val explode = bed.of(TreasureEventType.EXPLODE).first()
        assertEquals(3600L, explode.atMs)
        assertEquals(plant.bombNo, explode.bombNo)
        assertEquals(listOf(4 to 0), explode.blocksHit!!.map { it.i to it.j })
        assertEquals(true, explode.mapNowEmpty)
        assertTrue(bed.auto.awaitingNewMap)
    }

    @Test
    fun `after planting the hero leaves for its next target at the same instant`() {
        val bed = Bed(session(block(2, 0, hp = 99), block(0, 4, hp = 99)), spawns = listOf(0 to 2))
        bed.start(listOf(hero(1, speed = 10, bombs = 3)))
        bed.runUntil(1_000)

        val plant = bed.of(TreasureEventType.PLANT).first()
        val nextMove = bed.events.first { it.seq > plant.seq }
        assertEquals(TreasureEventType.MOVE, nextMove.type)
        assertEquals(plant.atMs, nextMove.atMs)
        assertEquals(plant.i to plant.j, nextMove.i to nextMove.j)
    }

    // Events after [from] must be heroes standing on one cell each, re-planting there every fuse.
    private fun assertCamping(bed: Bed, from: Long, fuseMs: Long, cells: Set<Cell>) {
        val late = bed.events.filter { it.atMs > from }
        assertEquals(emptyList(), late.filter { it.type == TreasureEventType.MOVE && !it.path.isNullOrEmpty() }, "nobody moves once the cells are taken")
        val plants = late.filter { it.type == TreasureEventType.PLANT }.groupBy { it.heroId!! }
        assertEquals(cells, plants.values.map { list -> list.map { it.i!! to it.j!! }.distinct().single() }.toSet())
        assertEquals(cells.size, plants.size, "one hero per cell")
        for (list in plants.values) {
            assertTrue(list.size >= 3)
            assertTrue(list.zipWithNext().all { (a, b) -> b.atMs == a.atMs + fuseMs }, "re-plants the instant its bomb explodes")
        }
    }

    @Test
    fun `on the last brick the heroes next to it stay and re-plant, everyone else stands still`() {
        // Brick (5,2) sits between two walls: only (4,2) and (6,2) touch it.
        val blocks = listOf(block(5, 2, hp = 9999))
        val spawns = listOf(0 to 2, 10 to 2, 0 to 10, 20 to 10, 30 to 16)
        val bed = Bed(session(*blocks.toTypedArray()), spawns, fuseMs = 1000)
        val heroes = (1..5).map { hero(it, speed = 5, bombs = 2) }
        val snapshot = bed.start(heroes)
        bed.runUntil(30_000)

        assertCamping(bed, from = 3_000, fuseMs = 1000, cells = setOf(4 to 2, 6 to 2))
        val replay = TreasureReplay(snapshot, capacity = heroes.associate { it.hero.heroId to it.bombCount })
        replay.apply(bed.events)
        assertEquals(emptyList(), replay.errors)
    }

    @Test
    fun `a hero out of bombs on its next cell roams until its bomb explodes, then plants there`() {
        // Two bricks, so the hero moves on after planting and waits on the new cell for its only bomb.
        val bed = Bed(session(block(4, 0, hp = 9999), block(8, 0, hp = 9999)), spawns = listOf(0 to 0))
        bed.start(listOf(hero(1, speed = 5, bombs = 1)))
        bed.runUntil(4_000)
        val (first, second) = bed.of(TreasureEventType.PLANT)
        val roam = bed.of(TreasureEventType.MOVE).first { it.roamUntilMs != null }
        assertEquals(first.explodeAtMs, roam.roamUntilMs)
        assertEquals(emptyList(), roam.path)
        assertTrue(roam.i to roam.j != first.i to first.j, "waits on a new cell")
        assertEquals(roam.i to roam.j, second.i to second.j)
        assertEquals(roam.roamUntilMs, second.atMs, "plants the instant it is back")
    }

    @Test
    fun `a hero camping on the last brick is never told to roam`() {
        val bed = Bed(session(block(4, 0, hp = 9999)), spawns = listOf(0 to 0), fuseMs = 1000)
        bed.start(listOf(hero(1, speed = 5, bombs = 1)))
        bed.runUntil(10_000)
        assertTrue(bed.of(TreasureEventType.PLANT).size >= 5)
        assertTrue(bed.of(TreasureEventType.MOVE).none { it.roamUntilMs != null })
    }

    @Test
    fun `a roaming hero is told to stand when the player pauses`() {
        val bed = Bed(session(block(4, 0, hp = 9999), block(8, 0, hp = 9999)), spawns = listOf(0 to 0))
        bed.start(listOf(hero(1, speed = 5, bombs = 1)))
        var now = 0L
        while (bed.of(TreasureEventType.MOVE).none { it.roamUntilMs != null }) bed.runUntil(++now * 100)
        val until = bed.of(TreasureEventType.MOVE).last().roamUntilMs!!
        bed.auto.setPaused(true, now * 100)
        bed.runUntil(10_000)
        val stand = bed.of(TreasureEventType.MOVE).last()
        assertEquals(until, stand.atMs)
        assertEquals(null, stand.roamUntilMs)
        assertEquals(emptyList(), stand.path)
        assertEquals(1, bed.of(TreasureEventType.PLANT).size)
    }

    @Test
    fun `a lone hero on the last brick stays on its cell instead of walking around the brick`() {
        val bed = Bed(session(block(4, 0, hp = 9999)), spawns = listOf(0 to 0), fuseMs = 1000)
        bed.start(listOf(hero(1, speed = 5, bombs = 3)))
        bed.runUntil(10_000)
        assertEquals(1, bed.walks().size)
        assertTrue(bed.of(TreasureEventType.PLANT).all { it.i == 3 && it.j == 0 })
        assertEquals(10, bed.of(TreasureEventType.PLANT).size)
    }

    @Test
    fun `with two bricks left nobody camps, heroes keep picking targets after planting`() {
        val blocks = listOf(block(5, 2, hp = 9999), block(5, 6, hp = 9999))
        val spawns = listOf(0 to 2, 10 to 2, 0 to 6, 10 to 6, 20 to 10, 30 to 16, 34 to 0)
        val bed = Bed(session(*blocks.toTypedArray()), spawns, fuseMs = 1000)
        val heroes = (1..7).map { hero(it, speed = 5, bombs = 1) }
        val snapshot = bed.start(heroes)
        bed.runUntil(40_000)

        // No hero holds its cell: a planted hero has no target until it picks one like everyone else.
        val late = bed.events.filter { it.atMs > 10_000 }
        assertTrue(late.any { it.type == TreasureEventType.MOVE && !it.path.isNullOrEmpty() })
        assertTrue(late.count { it.type == TreasureEventType.PLANT } >= 30)
        val replay = TreasureReplay(snapshot, capacity = heroes.associate { it.hero.heroId to it.bombCount })
        replay.apply(bed.events)
        assertEquals(emptyList(), replay.errors)
    }

    @Test
    fun `a hero at bomb capacity waits for its own bomb to explode before planting again`() {
        // Two targets one tile apart, a 1-bomb hero: the second plant must wait for the first bomb.
        val bed = Bed(session(block(0, 2, hp = 99), block(2, 2, hp = 99)), spawns = listOf(0 to 1), fuseMs = 1000)
        bed.start(listOf(hero(1, speed = 10, bombs = 1)))
        bed.runUntil(5_000)

        val plants = bed.of(TreasureEventType.PLANT)
        val explodes = bed.of(TreasureEventType.EXPLODE)
        assertTrue(plants.size >= 2)
        val first = plants[0]
        val second = plants[1]
        val firstExplode = explodes.first { it.bombNo == first.bombNo }
        assertEquals(first.atMs + 1000, firstExplode.atMs)
        assertTrue(second.atMs >= firstExplode.atMs, "second plant ${second.atMs} before first bomb went off ${firstExplode.atMs}")
        assertTrue(firstExplode.seq < second.seq)
        assertTrue(bed.auto.run { liveBombCount } <= 1)
    }

    @Test
    fun `walls and live bombs block the path, a block-pass hero walks through bricks`() {
        // Hero walled in by bricks at (1,0) and (0,1); the nearest targets (2,0) and (0,2) are only reachable through a brick.
        val s = session(block(1, 0, hp = 99), block(0, 1, hp = 99), block(3, 0, hp = 99))
        val bed = Bed(s, spawns = listOf(0 to 0))
        bed.start(listOf(hero(1, blockPass = true)))
        bed.runUntil(100)
        val move = bed.of(TreasureEventType.MOVE).first()
        assertTrue(move.path!!.any { it.i == 1 && it.j == 0 } || move.path!!.any { it.i == 0 && it.j == 1 }, "walks through a brick: ${move.path}")
    }

    @Test
    fun `targets with no path are given up at once, a walled-in hero falls back to its own tile`() {
        // Target (2,0) sits behind brick (1,0) for a normal hero standing at (0,0) walled by (0,1).
        val s0Blocks = listOf(block(1, 0, hp = 999), block(0, 1, hp = 999), block(3, 0, hp = 999), block(0, 3, hp = 999))
        val s = session(*s0Blocks.toTypedArray())
        val bed = Bed(s, spawns = listOf(0 to 0), fuseMs = 60_000)
        bed.start(listOf(hero(1, bombs = 1)))
        bed.runUntil(10_000)
        // Every other candidate is behind a brick: no MOVE into a brick ever, it plants where it stands.
        assertEquals(Triple(0, 0, 0L), bed.of(TreasureEventType.PLANT).first().let { Triple(it.i!!, it.j!!, it.atMs) })
        val start = AutoSnapshotDto(0, 0, 60_000, listOf(HeroPositionDto(1, 0, 0)), emptyList(), s0Blocks)
        val replay = TreasureReplay(start)
        replay.apply(bed.events)
        assertEquals(emptyList(), replay.errors)
        assertTrue(bed.walks().isEmpty(), "never walks into bricks")
    }

    @Test
    fun `a target whose brick was destroyed on the way is dropped at the next tile`() {
        // Two heroes; hero 2 destroys the only brick next to hero 1's target while hero 1 walks.
        val s = session(block(10, 0, hp = 1), block(20, 0, hp = 50))
        val bed = Bed(s, spawns = listOf(9 to 0, 0 to 0), fuseMs = 100)
        bed.start(listOf(hero(2, speed = 1, bombs = 1), hero(1, speed = 1, bombs = 1)))
        bed.runUntil(30_000)
        val replay = TreasureReplay(
            AutoSnapshotDto(0, 0, 100, listOf(HeroPositionDto(2, 9, 0), HeroPositionDto(1, 0, 0)), emptyList(), listOf(block(10, 0, 1), block(20, 0, 50))),
            capacity = mapOf(1 to 1, 2 to 1),
        )
        replay.apply(bed.events)
        assertEquals(emptyList(), replay.errors)
    }

    @Test
    fun `explode applies damage and rolls rewards through the session`() {
        val reward = RewardConfig(rewardTables = mapOf("TR-${GameConstants.BlockType.WOODEN}" to listOf(RewardEntry("COIN", 1, 2f, 2f))))
        val bed = Bed(session(block(1, 0, hp = 10), reward = reward), spawns = listOf(3 to 0), fuseMs = 500)
        bed.start(listOf(hero(1)))
        bed.runUntil(1_000)
        val hit = bed.of(TreasureEventType.EXPLODE).single().blocksHit!!.single()
        assertEquals(0, hit.hp, "damageTreasure 10 destroys an hp-10 block")
        assertEquals("COIN", hit.rewards.single().type)
        assertNull(bed.session.map.getBlock(1, 0))
    }

    @Test
    fun `map clear halts everyone, map replace respawns them and they resume after the pause`() {
        val s = session(block(1, 0))
        val bed = Bed(s, spawns = listOf(3 to 0, 4 to 0, 6 to 4, 5 to 4), fuseMs = 100, pauseMs = 2000)
        bed.start(listOf(hero(1), hero(2)))
        bed.runUntil(1_000)
        assertTrue(bed.auto.awaitingNewMap)

        s.replaceMap(MapGrid.fromDtos(listOf(block(7, 4)), 0, "PVE_V2"), now = 1_000)
        val newMap = bed.runUntil(1_000).single { it.type == TreasureEventType.NEW_MAP }
        assertEquals(3_000L, newMap.resumeAtMs)
        assertEquals(setOf(1, 2), newMap.heroes!!.map { it.heroId }.toSet())
        bed.runUntil(10_000)
        val firstAfter = bed.events.first { it.seq > newMap.seq }
        assertTrue(firstAfter.atMs >= 3_000, "nobody moves during the pause")
        assertTrue(bed.of(TreasureEventType.PLANT).any { it.i == 7 || it.i == 6 || it.i == 8 })
    }

    @Test
    fun `roster changes emit HERO_JOIN and HERO_LEAVE, a leaving hero's bomb still explodes`() {
        val bed = Bed(session(block(1, 0, hp = 99), block(9, 4, hp = 99)), spawns = listOf(3 to 0, 8 to 4), fuseMs = 1000)
        bed.start(listOf(hero(1)))
        bed.runUntil(250)
        bed.auto.removeHeroes(listOf(1), "energy", 250)
        bed.auto.upsertHeroes(listOf(hero(2)), 250)
        bed.runUntil(5_000)

        val leave = bed.of(TreasureEventType.HERO_LEAVE).single()
        assertEquals("energy", leave.reason)
        val join = bed.of(TreasureEventType.HERO_JOIN).single()
        assertEquals(8 to 4, join.i to join.j)
        assertTrue(bed.of(TreasureEventType.EXPLODE).any { it.heroId == 1 }, "hero 1's bomb still goes off")
        assertTrue(bed.events.none { it.heroId == 1 && it.seq > leave.seq && it.type != TreasureEventType.EXPLODE })
        assertEquals(setOf(2), bed.auto.heroIds)
    }

    @Test
    fun `a hero that leaves and rejoins never reuses the number of its still-live bomb`() {
        val bed = Bed(session(block(1, 0, hp = 99), block(9, 4, hp = 99)), spawns = listOf(3 to 0, 8 to 4), fuseMs = 5000)
        bed.start(listOf(hero(1)))
        bed.runUntil(250)
        bed.auto.removeHeroes(listOf(1), "not_working", 250)
        bed.auto.upsertHeroes(listOf(hero(1)), 250)
        // Its old bomb still counts against its capacity of 1, so the second plant waits for it.
        bed.runUntil(6_000)
        val plants = bed.of(TreasureEventType.PLANT).filter { it.heroId == 1 }
        assertEquals(listOf(0, 1), plants.take(2).map { it.bombNo })
        assertEquals(5_200L, plants[1].atMs)
    }

    @Test
    fun `restart is a resync - live bombs and positions survive, seq keeps counting`() {
        val bed = Bed(session(block(1, 0, hp = 99), block(9, 0, hp = 99)), spawns = listOf(3 to 0), fuseMs = 5000)
        bed.start(listOf(hero(1)))
        bed.runUntil(250)
        val before = bed.auto.seq
        val snapshot = bed.start(listOf(hero(1)), now = 250)
        assertEquals(before, snapshot.seq)
        assertEquals(1, snapshot.bombs.size)
        assertEquals(1, snapshot.heroes.size)
        bed.runUntil(10_000)
        assertTrue(bed.events.map { it.seq }.zipWithNext().all { (a, b) -> b == a + 1 })
    }

    @Test
    fun `fifteen heroes clearing a random map produce a stream a client can replay exactly`() {
        val random = Random(42)
        val blocks = mutableListOf<BlockDto>()
        for (i in 0 until GameConstants.MAP_MAX_COL) for (j in 0 until GameConstants.MAP_MAX_ROW) {
            if (i % 2 == 1 && j % 2 == 1) continue
            if (random.nextDouble() < 0.35) blocks.add(block(i, j, hp = random.nextInt(1, 30)))
        }
        val s = session(*blocks.toTypedArray())
        val empties = s.map.emptyCells().shuffled(random)
        val heroes = (1..15).map { hero(it, speed = random.nextInt(1, 9), bombs = random.nextInt(1, 4), blockPass = it % 5 == 0) }
        val bed = Bed(s, spawns = empties.take(15), fuseMs = 3000)
        val snapshot = bed.start(heroes)
        bed.runUntil(600_000)

        val replay = TreasureReplay(
            snapshot,
            blockPass = heroes.filter { it.blockPass }.map { it.hero.heroId }.toSet(),
            capacity = heroes.associate { it.hero.heroId to it.bombCount },
        )
        replay.apply(bed.events)
        assertEquals(emptyList(), replay.errors.take(10))
        assertTrue(replay.plants > 50, "plants=${replay.plants}")
        assertEquals(replay.plants, replay.explodes)
        assertTrue(bed.session.map.isEmpty(), "the heroes clear the whole map (${bed.session.map.blocks.size} left)")
        assertNotNull(bed.of(TreasureEventType.EXPLODE).last().mapNowEmpty?.takeIf { it })
    }

    @Test
    fun `pause halts heroes at their next tile, live bombs still explode, resume walks on`() {
        // Hero walks (0,0) -> (3,0) at 200 ms/tile, brick at (6,0) so it has a second target after planting.
        val bed = Bed(session(block(4, 0, hp = 99), block(6, 0, hp = 99)), spawns = listOf(0 to 0), fuseMs = 1000)
        bed.start(listOf(hero(1, speed = 5, bombs = 2)))
        bed.runUntil(700)
        val plant = bed.of(TreasureEventType.PLANT).single()
        assertEquals(600L, plant.atMs)

        bed.auto.setPaused(true, 700)
        bed.runUntil(10_000)
        // Mid-step at 700: stops on the tile it reaches at 800.
        val halt = bed.of(TreasureEventType.MOVE).last()
        assertEquals(800L, halt.atMs)
        assertEquals(emptyList(), halt.path)
        assertEquals(1600L, bed.of(TreasureEventType.EXPLODE).single().atMs, "planted bomb still explodes on time")
        assertEquals(1, bed.of(TreasureEventType.PLANT).size)
        assertTrue(bed.events.none { it.atMs > 1600 })

        bed.auto.setPaused(false, 10_000)
        bed.runUntil(20_000)
        val resumed = bed.events.first { it.atMs > 1600 }
        assertEquals(TreasureEventType.MOVE, resumed.type)
        assertEquals(10_000L, resumed.atMs)
        assertEquals(halt.i to halt.j, resumed.i to resumed.j)
        assertTrue(bed.of(TreasureEventType.PLANT).size > 1)
        assertTrue(bed.events.map { it.seq }.zipWithNext().all { (a, b) -> b == a + 1 })
    }

    @Test
    fun `starting paused keeps heroes still until resumed, a resync keeps the pause`() {
        val bed = Bed(session(block(4, 0, hp = 99)), spawns = listOf(0 to 0))
        bed.auto.start(listOf(hero(1)), bed.auto.config, 0, paused = true)
        bed.runUntil(5_000)
        assertEquals(emptyList(), bed.events)

        val snapshot = bed.auto.start(listOf(hero(1)), bed.auto.config, 5_000, paused = true)
        assertTrue(snapshot.paused)
        bed.runUntil(10_000)
        assertEquals(emptyList(), bed.events)

        bed.auto.setPaused(false, 10_000)
        bed.runUntil(10_000)
        assertEquals(10_000L, bed.of(TreasureEventType.MOVE).first().atMs)
    }
}
