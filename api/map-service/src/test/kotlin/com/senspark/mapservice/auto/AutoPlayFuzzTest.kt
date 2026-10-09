package com.senspark.mapservice.auto

import com.senspark.mapservice.domain.GameConstants
import com.senspark.mapservice.domain.MapGrid
import com.senspark.mapservice.domain.Session
import com.senspark.mapservice.domain.SessionConfig
import com.senspark.mapservice.model.AutoHeroDto
import com.senspark.mapservice.model.BlockDto
import com.senspark.mapservice.stresstest.TreasureReplay
import com.senspark.mapservice.testHero
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

// Random maps, heroes, fuses and roster churn: the stream must always replay, and maps must always clear.
class AutoPlayFuzzTest {
    private fun randomBlocks(random: Random, density: Double) = buildList {
        for (i in 0 until GameConstants.MAP_MAX_COL) for (j in 0 until GameConstants.MAP_MAX_ROW) {
            if (i % 2 == 1 && j % 2 == 1) continue
            if (random.nextDouble() < density) {
                val hp = random.nextInt(1, 60)
                add(BlockDto(i, j, random.nextInt(1, 8), hp, hp))
            }
        }
    }

    private fun run(seed: Int, churn: Boolean): List<String> {
        val random = Random(seed)
        val session = Session(MapGrid.fromDtos(randomBlocks(random, random.nextDouble(0.2, 0.7)), 0, "PVE_V2"), SessionConfig())
        val heroes = (1..random.nextInt(1, 16)).map {
            AutoHeroDto(
                testHero(it).copy(bombRange = random.nextInt(1, 5), pierceBlock = random.nextBoolean()),
                speed = random.nextInt(0, 12), bombCount = random.nextInt(0, 6), blockPass = random.nextInt(4) == 0,
            )
        }
        val auto = AutoPlay(session, AutoConfig(random.nextLong(200, 4000), 1000), Random(seed))
        session.autoPlay = auto
        val replay = TreasureReplay(
            auto.start(heroes, auto.config, 0),
            blockPass = heroes.filter { it.blockPass }.map { it.hero.heroId }.toSet(),
            capacity = heroes.associate { it.hero.heroId to maxOf(1, it.bombCount) },
        )
        var t = 0L
        var maps = 0
        while (t < 3_600_000 && maps < 2) {
            t += random.nextLong(1, 700)
            auto.advance(t)
            if (churn && random.nextInt(200) == 0) auto.removeHeroes(listOf(heroes.random(random).hero.heroId), "churn", t)
            if (churn && random.nextInt(200) == 0) auto.upsertHeroes(listOf(heroes.random(random)), t)
            if (auto.awaitingNewMap) {
                val blocks = randomBlocks(random, 0.3)
                session.replaceMap(MapGrid.fromDtos(blocks, 0, "PVE_V2"), t)
                replay.apply(auto.drainEvents())
                replay.loadBlocks(blocks)
                maps++
            }
            replay.apply(auto.drainEvents())
        }
        val problems = replay.errors.take(3).map { "seed=$seed $it" }.toMutableList()
        if (!churn && maps < 2) problems.add("seed=$seed stuck: ${session.map.blocks.size} blocks left after ${t}ms")
        return problems
    }

    @Test
    fun `every stream replays cleanly and no hero ever gets stuck`() {
        val problems = (1..12).flatMap { run(it, churn = false) } + (100..111).flatMap { run(it, churn = true) }
        assertEquals(emptyList(), problems)
    }
}
