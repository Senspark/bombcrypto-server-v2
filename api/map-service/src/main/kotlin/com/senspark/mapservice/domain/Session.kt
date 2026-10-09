package com.senspark.mapservice.domain

import com.senspark.mapservice.auto.AutoPlay
import com.senspark.mapservice.model.HeroSnapshotDto
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.random.Random

enum class TakeBombResult {
    OK,
    ALREADY_TAKEN,
    CANNOT_SET_BOOM,
}

data class BlockExplodeResult(
    val i: Int,
    val j: Int,
    val hp: Int,
    val type: Int,
    val rewards: List<Pair<String, Float>> = emptyList(),
)

data class ExplodeOutcome(
    val takeResult: TakeBombResult,
    val blocksHit: List<BlockExplodeResult> = emptyList(),
    val mapNowEmpty: Boolean = false,
)

private data class PlantedBomb(val bombNo: Int, val cell: Pair<Int, Int>, val plantedAt: Long)

/**
 * Per-user-session in-memory state and pure calculation logic, ported from
 * `com.senspark.game.manager.blockMap.UserBlockMapManagerImpl` (BombChainExtension). One instance
 * replaces what used to be one JVM object per logged-in user; here it's one entry in
 * [SessionStore] keyed by `"$userId-$dataType-$mode"`, with its own [lock] standing in for the
 * original per-instance `locker` monitor -- same locking granularity, no throughput regression.
 *
 * Deliberately excluded (stays on the main server, see the plan doc):
 * - Hero energy/dangerous/kill mutation (`bbm.subEnergy()`, `killBomberman()`, etc.)
 * - Wallet crediting (`IUserBlockRewardManager.addRewards`) and DB persistence (`saveLater`)
 * - Cross-user treasure-hunt pool registration (`ITreasureHuntV2Manager.addHeroToPool`)
 */
class Session(
    @Volatile var map: MapGrid,
    @Volatile var config: SessionConfig,
) {
    val lock = ReentrantLock()

    @Volatile
    var lastActivityMs: Long = System.currentTimeMillis()
        private set

    private val heroTargets: MutableMap<Int, Pair<Int, Int>> = ConcurrentHashMap()
    private val lastPlantCell: MutableMap<Int, Pair<Int, Int>> = ConcurrentHashMap()
    private val plantedBombs: MutableMap<Int, MutableList<PlantedBomb>> = ConcurrentHashMap()
    private val rejectedCells: MutableMap<Int, MutableSet<Pair<Int, Int>>> = ConcurrentHashMap()
    private val random = Random.Default

    // Bumped whenever a block or bomb changes, so auto-play can skip re-validating unchanged paths.
    var version: Long = 0
        private set

    // Server-driven treasure mode for this session; null until `/auto/start`.
    var autoPlay: AutoPlay? = null

    val isAutoRunning: Boolean get() = autoPlay?.running == true

    fun touch() {
        lastActivityMs = System.currentTimeMillis()
    }

    /** Replaces the map (a map-clear reload) and resets all per-hero tracking, exactly like the
     * original `createNewMap()`'s reset block -- every tracked coordinate is only valid against the
     * map it was picked from. */
    fun replaceMap(newMap: MapGrid, now: Long = System.currentTimeMillis()) = lock.withLock {
        autoPlay?.advance(now)
        map = newMap
        heroTargets.clear()
        lastPlantCell.clear()
        plantedBombs.clear()
        rejectedCells.clear()
        version++
        autoPlay?.onMapReplaced(now)
    }

    // ================== target picking ==================

    /**
     * Strictest BFS first, then progressively drops exclusions (other heroes' targets, then this
     * hero's rejects, then reachability itself) rather than ever answering "no target".
     */
    private fun findTargetWithFallbacks(
        heroId: Int,
        fromCell: Pair<Int, Int>,
        occupied: Set<Pair<Int, Int>> = emptySet(),
    ): Pair<Int, Int>? {
        val bombCells = plantedBombCells() + occupied
        val otherTargets = otherHeroTargets(heroId)
        val rejected = rejectedCells[heroId] ?: emptySet()

        map.findBombTarget(fromCell.first, fromCell.second, bombCells + otherTargets + rejected)
            ?.let { return it }

        if (otherTargets.isNotEmpty()) {
            map.findBombTarget(fromCell.first, fromCell.second, bombCells + rejected)?.let { return it }
        }
        if (rejected.isNotEmpty()) {
            map.findBombTarget(fromCell.first, fromCell.second, bombCells)?.let {
                rejectedCells.remove(heroId)
                return it
            }
        }
        map.findAnyBombTarget(fromCell.first, fromCell.second, bombCells + rejected)?.let { return it }
        if (rejected.isNotEmpty()) {
            map.findAnyBombTarget(fromCell.first, fromCell.second, bombCells)?.let {
                rejectedCells.remove(heroId)
                return it
            }
        }
        return null
    }

    private fun rejectTargetLocked(heroId: Int, col: Int, row: Int) {
        val cell = col to row
        if (heroTargets[heroId] == cell) {
            heroTargets.remove(heroId)
        }
        rejectedCells.getOrPut(heroId) { ConcurrentHashMap.newKeySet() }.add(cell)
    }

    private fun otherHeroTargets(excludeHeroId: Int): Set<Pair<Int, Int>> {
        val result = mutableSetOf<Pair<Int, Int>>()
        for ((hid, pos) in heroTargets) {
            if (hid != excludeHeroId) result.add(pos)
        }
        return result
    }

    private fun plantedBombCells(): Set<Pair<Int, Int>> {
        val result = mutableSetOf<Pair<Int, Int>>()
        for (bombs in plantedBombs.values) {
            for (bomb in bombs) result.add(bomb.cell)
        }
        return result
    }

    private fun takePlantedBomb(heroId: Int, bombNo: Int, col: Int, row: Int): Boolean {
        val bombs = plantedBombs[heroId] ?: return false
        val index = bombs.indexOfFirst { it.bombNo == bombNo && it.cell == (col to row) }
        if (index < 0) return false
        bombs.removeAt(index)
        return true
    }

    // ================== auto-play helpers (caller holds [lock]) ==================

    internal fun holdAutoTarget(heroId: Int, cell: Pair<Int, Int>) {
        heroTargets[heroId] = cell
    }

    internal fun releaseAutoTarget(heroId: Int) {
        heroTargets.remove(heroId)
    }

    /** Records a server-decided plant; the caller already checked the cell and bomb capacity. */
    internal fun recordAutoPlant(heroId: Int, bombNo: Int, cell: Pair<Int, Int>, now: Long) {
        plantedBombs.getOrPut(heroId) { mutableListOf() }.add(PlantedBomb(bombNo, cell, now))
        lastPlantCell[heroId] = cell
        heroTargets.remove(heroId)
        rejectedCells.remove(heroId)
        version++
    }

    internal fun hasBombAt(cell: Pair<Int, Int>): Boolean = plantedBombs.values.any { list -> list.any { it.cell == cell } }

    internal fun forgetHero(heroId: Int) {
        heroTargets.remove(heroId)
        lastPlantCell.remove(heroId)
        rejectedCells.remove(heroId)
    }

    // ================== explode ==================

    /**
     * Ported from `explode()`/`startExplodeLegacy`/`startExplodeTon`'s block-hit slice only: blast
     * radius (`getBlockExplode`), HP subtraction, block removal, and the reward roll. Excludes hero
     * energy/dangerous/kill mutation, wallet crediting, and treasure-hunt pool registration -- see
     * the class doc. Always takes the planted bomb first, so the hero's live-bomb slot in
     * [plantedBombs] is released even when nothing is hit.
     */
    fun explode(bombNo: Int, col: Int, row: Int, hero: HeroSnapshotDto): ExplodeOutcome = lock.withLock {
        val taken = takePlantedBomb(hero.heroId, bombNo, col, row)
        if (!taken) {
            return@withLock ExplodeOutcome(TakeBombResult.ALREADY_TAKEN)
        }
        version++
        if (!map.canSetBoom(col, row)) {
            return@withLock ExplodeOutcome(TakeBombResult.CANNOT_SET_BOOM)
        }

        val blocks = getBlockExplode(hero, col, row)
        val damTreasure = hero.damageTreasure + hero.totalPower
        val damJail = hero.damageJail + hero.totalPower

        val blocksHit = mutableListOf<BlockExplodeResult>()

        for (bm in blocks) {
            if (bm.type == GameConstants.BlockType.JAIL) {
                bm.subHp(damJail)
            } else {
                bm.subHp(damTreasure)
            }

            val rewards = if (bm.hp <= 0) {
                map.removeBlock(bm)
                rollRewards(hero, bm.type)
            } else {
                emptyList()
            }
            blocksHit.add(BlockExplodeResult(bm.i, bm.j, bm.hp, bm.type, rewards))
        }

        val mapNowEmpty = map.isEmpty()
        ExplodeOutcome(TakeBombResult.OK, blocksHit, mapNowEmpty)
    }

    private fun getBlockExplode(hero: HeroSnapshotDto, colBoom: Int, rowBoom: Int): List<Block> {
        val blocks = mutableListOf<Block>()
        val bombRange = hero.bombRange
        val pierceBlock = hero.pierceBlock
        var block: Block?

        var minCol = (colBoom - bombRange).coerceAtLeast(0)
        for (col in colBoom - 1 downTo minCol) {
            if (col % 2 == 1 && rowBoom % 2 == 1) break
            block = map.getBlock(col, rowBoom)
            if (block != null) blocks.add(block)
            if (block != null && !pierceBlock) break
        }

        var maxCol = (colBoom + bombRange).let { if (it >= GameConstants.MAP_MAX_COL) GameConstants.MAP_MAX_COL - 1 else it }
        for (col in colBoom + 1..maxCol) {
            if (col % 2 == 1 && rowBoom % 2 == 1) break
            block = map.getBlock(col, rowBoom)
            if (block != null) blocks.add(block)
            if (block != null && !pierceBlock) break
        }

        var minRow = (rowBoom - bombRange).coerceAtLeast(0)
        for (row in rowBoom - 1 downTo minRow) {
            if (colBoom % 2 == 1 && row % 2 == 1) break
            block = map.getBlock(colBoom, row)
            if (block != null) blocks.add(block)
            if (block != null && !pierceBlock) break
        }

        var maxRow = (rowBoom + bombRange).let { if (it >= GameConstants.MAP_MAX_ROW) GameConstants.MAP_MAX_ROW - 1 else it }
        for (row in rowBoom + 1..maxRow) {
            if (colBoom % 2 == 1 && row % 2 == 1) break
            block = map.getBlock(colBoom, row)
            if (block != null) blocks.add(block)
            if (block != null && !pierceBlock) break
        }
        return blocks
    }

    /**
     * Ported from `UserBlockMapManagerImpl.getRewards` (legacy) / the inline reward-type list in
     * `startExplodeTon` (airdrop): decide which reward types this hero can roll for this block type,
     * then delegate the actual weighted roll to the session's reward table -- mirrors
     * `BlockRewardDataManager.getRewards` exactly (weights sorted ascending upstream is NOT required
     * here since [WeightedRandom] doesn't care about order).
     */
    private fun rollRewards(hero: HeroSnapshotDto, blockType: Int): List<Pair<String, Float>> {
        val reward = config.reward
        val allowedTypes: MutableSet<String> = mutableSetOf("COIN", "BOMBERMAN")
        if (!hero.isAirdropUser) {
            var stakeBcoin = hero.stakeBcoin
            if (!hero.isHeroS) {
                stakeBcoin -= (reward.minStakeHeroConfig[hero.rarity] ?: 0).toDouble()
            }
            val minBcoin = reward.minStakeBcoinTHV1.getOrElse(hero.rarity) { 0 }
            if (stakeBcoin >= minBcoin) {
                allowedTypes.add("BCOIN")
            }
            val minSen = reward.minStakeSenTHV1.getOrElse(hero.rarity) { 0 }
            if (hero.stakeSen >= minSen) {
                allowedTypes.add("SENSPARK")
            }
        }

        // Matches BlockRewardDataManager's own key shape ("$dataType-$blockType") exactly, using
        // the caller-supplied dataType string verbatim -- no guessing/suffix-matching.
        val key = "${hero.dataType}-$blockType"
        val entries = reward.rewardTables[key] ?: emptyList()

        val eligible = entries.filter { allowedTypes.contains(it.type) }
        if (eligible.isEmpty()) return emptyList()
        val totalWeight = eligible.sumOf { it.weight }
        if (totalWeight <= 0) return emptyList()

        val randomizer = WeightedRandom(eligible.map { it.weight.toFloat() })
        val picked = eligible[randomizer.random(random)]
        val value = if (picked.maxValue > picked.minValue) {
            picked.minValue + random.nextFloat() * (picked.maxValue - picked.minValue)
        } else {
            picked.minValue
        }
        if (value <= 0f) return emptyList()
        return listOf(picked.type to value)
    }
}
