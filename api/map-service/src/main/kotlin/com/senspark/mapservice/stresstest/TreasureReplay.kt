package com.senspark.mapservice.stresstest

import com.senspark.mapservice.auto.Cell
import com.senspark.mapservice.domain.GameConstants
import com.senspark.mapservice.model.AutoSnapshotDto
import com.senspark.mapservice.model.BlockDto
import com.senspark.mapservice.model.TreasureEventDto
import com.senspark.mapservice.model.TreasureEventType
import kotlin.math.abs

// Re-enacts the event stream like the client does and records anything a client couldn't replay
// (seq gaps, off-tile MOVE, steps into walls/bricks/bombs, early or off-path plants, fuse mismatch).
class TreasureReplay(
    snapshot: AutoSnapshotDto,
    private val blockPass: Set<Int> = emptySet(),
    private val capacity: Map<Int, Int> = emptyMap(),
) {
    private class Walk(val from: Cell, val path: List<Cell>, val startAt: Long, val stepMs: Long) {
        // Tile the hero stands on at [t], or null if it's between two tiles.
        fun tileAt(t: Long): Cell? {
            val elapsed = t - startAt
            if (elapsed < 0 || stepMs <= 0) return null
            if (elapsed % stepMs != 0L) return if (elapsed >= path.size * stepMs) path.lastOrNull() ?: from else null
            val k = (elapsed / stepMs).toInt()
            return if (k == 0) from else path.getOrElse(k - 1) { path.lastOrNull() ?: from }
        }

        fun endAt() = startAt + path.size * stepMs
        fun end() = path.lastOrNull() ?: from
    }

    private data class Bomb(val heroId: Int, val bombNo: Int, val cell: Cell, val explodeAt: Long)

    var lastSeq = snapshot.seq
        private set
    private var lastAt = Long.MIN_VALUE
    private val blocks = HashMap<Cell, Int>()
    private val heroes = HashMap<Int, Walk>()
    private val bombs = ArrayList<Bomb>()
    val errors = ArrayList<String>()

    var plants = 0
        private set
    var explodes = 0
        private set
    var moves = 0
        private set
    var newMaps = 0
        private set
    var blocksDestroyed = 0
        private set

    init {
        loadBlocks(snapshot.blocks)
        for (h in snapshot.heroes) heroes[h.heroId] = Walk(h.i to h.j, emptyList(), snapshot.serverTimeMs, 1)
        for (b in snapshot.bombs) bombs.add(Bomb(b.heroId, b.bombNo, b.i to b.j, b.explodeAtMs))
    }

    fun loadBlocks(list: List<BlockDto>) {
        blocks.clear()
        for (b in list) blocks[b.i to b.j] = b.hp
    }

    fun heroPosition(heroId: Int): Cell? = heroes[heroId]?.end()

    fun apply(events: List<TreasureEventDto>) = events.forEach { apply(it) }

    fun apply(e: TreasureEventDto) {
        if (e.seq <= lastSeq) return // already reflected in the snapshot
        check(e.seq == lastSeq + 1, "seq gap: expected ${lastSeq + 1} got ${e.seq}")
        lastSeq = e.seq
        check(e.atMs >= lastAt, "time went backwards at seq ${e.seq}: ${e.atMs} < $lastAt")
        lastAt = maxOf(lastAt, e.atMs)
        when (e.type) {
            TreasureEventType.MOVE -> onMove(e)
            TreasureEventType.PLANT -> onPlant(e)
            TreasureEventType.EXPLODE -> onExplode(e)
            TreasureEventType.HERO_JOIN -> heroes[e.heroId!!] = Walk(e.i!! to e.j!!, emptyList(), e.atMs, 1)
            TreasureEventType.HERO_LEAVE -> heroes.remove(e.heroId!!)
            TreasureEventType.NEW_MAP -> {
                newMaps++
                bombs.clear()
                heroes.clear()
                for (h in e.heroes!!) heroes[h.heroId] = Walk(h.i to h.j, emptyList(), e.resumeAtMs!!, 1)
            }
            else -> errors.add("unknown type ${e.type}")
        }
    }

    private fun onMove(e: TreasureEventDto) {
        moves++
        val id = e.heroId!!
        val walk = heroes[id] ?: return errors.add("MOVE for unknown hero $id at seq ${e.seq}").let { }
        val from = e.i!! to e.j!!
        val here = walk.tileAt(e.atMs)
        check(here == from, "hero $id MOVE seq ${e.seq} from $from but it stands on $here at ${e.atMs}")
        val path = e.path!!.map { it.i to it.j }
        var prev = from
        path.forEachIndexed { k, cell ->
            check(abs(cell.first - prev.first) + abs(cell.second - prev.second) == 1, "hero $id non-adjacent step $prev->$cell")
            check(cell.first in 0 until GameConstants.MAP_MAX_COL && cell.second in 0 until GameConstants.MAP_MAX_ROW, "out of map $cell")
            check(!(cell.first % 2 == 1 && cell.second % 2 == 1), "hero $id walks into wall $cell")
            if (id !in blockPass) check(cell !in blocks, "hero $id walks into brick $cell (seq ${e.seq})")
            check(bombs.none { it.cell == cell }, "hero $id walks into a live bomb at $cell (seq ${e.seq}, step $k)")
            prev = cell
        }
        heroes[id] = Walk(from, path, e.atMs, e.stepMs!!)
    }

    private fun onPlant(e: TreasureEventDto) {
        plants++
        val id = e.heroId!!
        val cell = e.i!! to e.j!!
        val walk = heroes[id] ?: return errors.add("PLANT for unknown hero $id").let { }
        check(e.atMs >= walk.endAt(), "hero $id planted at ${e.atMs} before arriving at ${walk.endAt()}")
        check(walk.end() == cell, "hero $id planted at $cell but stands on ${walk.end()}")
        check(cell !in blocks, "plant on a brick $cell")
        check(bombs.none { it.cell == cell }, "plant on a live bomb $cell")
        val around = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1).any { (di, dj) -> (cell.first + di to cell.second + dj) in blocks }
        check(around, "plant at $cell with no brick around")
        capacity[id]?.let { cap -> check(bombs.count { it.heroId == id } < cap, "hero $id over bomb capacity $cap") }
        bombs.add(Bomb(id, e.bombNo!!, cell, e.explodeAtMs!!))
        heroes[id] = Walk(cell, emptyList(), e.atMs, walk.stepMs)
    }

    private fun onExplode(e: TreasureEventDto) {
        explodes++
        val bomb = bombs.firstOrNull { it.heroId == e.heroId && it.bombNo == e.bombNo }
        if (bomb == null) {
            errors.add("EXPLODE for unknown bomb hero=${e.heroId} no=${e.bombNo} seq=${e.seq}")
            return
        }
        bombs.remove(bomb)
        check(bomb.cell == (e.i!! to e.j!!), "explode cell mismatch")
        check(e.atMs == bomb.explodeAt, "bomb exploded at ${e.atMs}, fuse said ${bomb.explodeAt}")
        for (hit in e.blocksHit.orEmpty()) {
            if (hit.hp <= 0) {
                if (blocks.remove(hit.i to hit.j) != null) blocksDestroyed++
            } else {
                blocks[hit.i to hit.j] = hit.hp
            }
        }
    }

    private fun check(ok: Boolean, message: String) {
        if (!ok) errors.add(message)
    }
}
