package com.senspark.client.explodev6

import com.senspark.game.controller.MapData
import com.senspark.game.declare.GameConstants.MAP_MAX_COL
import com.senspark.game.declare.GameConstants.MAP_MAX_ROW

// Virtual by default; [RealClock] for the server's move-time check, which reads the wall clock.
interface TestClock {
    fun nowMs(): Long
    fun advance(ms: Long)
}

class VirtualClock(private var _now: Long = 1_000_000L) : TestClock {
    override fun nowMs() = _now
    override fun advance(ms: Long) {
        _now += ms
    }
}

class RealClock : TestClock {
    override fun nowMs() = System.currentTimeMillis()
    override fun advance(ms: Long) {
        if (ms > 0) Thread.sleep(ms)
    }
}

// Client-side map (Unity IMapManagerV2), kept separate from server [MapData] so tests can drift them.
class ClientMap(val bricks: MutableSet<Cell> = mutableSetOf()) {

    companion object {
        fun mirrorOf(map: MapData): ClientMap =
            ClientMap(map.blocks.map { cell(it.i, it.j) }.toMutableSet())
    }

    fun isWall(c: Cell): Boolean = c.i % 2 == 1 && c.j % 2 == 1

    fun inBounds(c: Cell): Boolean = c.i in 0 until MAP_MAX_COL && c.j in 0 until MAP_MAX_ROW

    fun hasBrick(c: Cell): Boolean = c in bricks

    fun hasBrickAround(c: Cell): Boolean =
        listOf(cell(c.i - 1, c.j), cell(c.i + 1, c.j), cell(c.i, c.j - 1), cell(c.i, c.j + 1))
            .any { it in bricks }

    fun removeBrick(c: Cell) {
        bricks.remove(c)
    }

    // Next step toward [to], or null when no route exists.
    fun nextStep(from: Cell, to: Cell, blockPass: Boolean): Cell? {
        if (from == to) return null
        val previous = mutableMapOf<Cell, Cell>()
        val queue = ArrayDeque<Cell>()
        queue.add(from)
        previous[from] = from
        while (queue.isNotEmpty()) {
            val at = queue.removeFirst()
            if (at == to) {
                var step = to
                while (previous[step] != from) step = previous[step]!!
                return step
            }
            for (n in listOf(cell(at.i - 1, at.j), cell(at.i + 1, at.j), cell(at.i, at.j - 1), cell(at.i, at.j + 1))) {
                if (!inBounds(n) || n in previous || isWall(n)) continue
                // Destination is always enterable.
                if (hasBrick(n) && !blockPass && n != to) continue
                previous[n] = at
                queue.add(n)
            }
        }
        return null
    }
}

// Hunter-mode hero driven like Unity's BotManager, including its guards (no target = no move,
// plant only on held target, reject unusable cells).
class SimulatedHero(
    val heroId: Int,
    val speed: Int,
    val bombCount: Int = 1,
    val bombRange: Int = 1,
    val blockPass: Boolean = false,
    var currentLocation: Cell,
    private val clientMap: ClientMap,
    private val targets: ClientBombTargetManager,
    private val client: FakeGameClient,
    private val clock: TestClock,
) {
    private data class LiveBomb(val bombNo: Int, val cell: Cell, val readyAtMs: Long)

    companion object {
        // Simulation pacing before firing the server fuse, not the real fuse.
        const val FuseMs = 3_000L
        const val UnreachableRejectDelayMs = 3_000L
    }

    private val _live = mutableListOf<LiveBomb>()
    private var _unreachableTarget: Cell? = null
    private var _unreachableSinceMs = 0L

    fun onNewMap(spawnAt: Cell) {
        _live.clear()
        _unreachableTarget = null
        currentLocation = spawnAt
    }

    var plantsAccepted = 0
        private set
    var plantsRejected = 0
        private set

    // Accepted plants with no next target (end of map); not a rejection.
    var plantsWithNoNextTarget = 0
        private set
    var explodesAccepted = 0
        private set

    // Fuse fired but server pushed nothing (hero guard failed).
    var explodesRejected = 0
        private set

    val explodeRejections = mutableListOf<Pair<Int, Cell>>()
    var rejectsSent = 0
        private set

    // (cell, server error or null) per plant attempt.
    val plantLog = mutableListOf<Pair<Cell, Int?>>()

    val liveBombCount get() = _live.size

    // One BotManager.OnUpdate; advances the clock by one tile of walking.
    fun tick() {
        deliverDueExplosions()

        val target = targets.tryGetTarget(heroId)
        if (target == null) {
            // Guard 1: no target, stand still and ask again.
            targets.requestTarget(heroId, currentLocation)
            targets.flush()
            clock.advance(stepMs())
            return
        }

        if (currentLocation == target) {
            checkToSpawnBomb(target)
            clock.advance(stepMs())
            return
        }

        val step = clientMap.nextStep(currentLocation, target, blockPass)
        if (step == null) {
            onTargetUnreachable(target)
            clock.advance(stepMs())
            return
        }
        currentLocation = step
        _unreachableTarget = null
        clock.advance(stepMs())
    }

    fun runUntil(maxTicks: Int = 2_000, predicate: () -> Boolean): Boolean {
        repeat(maxTicks) {
            if (predicate()) return true
            tick()
        }
        return predicate()
    }

    private fun checkToSpawnBomb(heldTarget: Cell) {
        // Guard 2: only plant on the held target.
        if (heldTarget != currentLocation) return
        if (_live.any { it.cell == currentLocation }) return
        if (_live.size >= bombCount) return

        if (!clientMap.hasBrickAround(currentLocation)) {
            // Guard 3: reject the cell, don't just drop it.
            targets.rejectTarget(heroId, currentLocation)
            rejectsSent++
            return
        }
        spawnBomb()
    }

    private fun spawnBomb() {
        val bombNo = nextBombId()
        val cellPlanted = currentLocation
        _live.add(LiveBomb(bombNo, cellPlanted, clock.nowMs() + FuseMs))
        targets.notifyPlanted(heroId)

        val result = client.startPlantBomb(heroId, bombNo, cellPlanted)
        plantLog.add(cellPlanted to result.errorCode)
        when {
            result.isOk -> {
                plantsAccepted++
                targets.onBombTarget(heroId, result.requireNextTarget())
            }
            result.isNoTarget -> {
                // Bomb was planted, just no next target; clean up like a mismatch to re-request.
                plantsAccepted++
                plantsWithNoNextTarget++
                targets.onBombTargetMismatch(heroId)
            }
            else -> {
                plantsRejected++
                targets.onBombTargetMismatch(heroId)
            }
        }
    }

    // Bombable.GetNextBombId: lowest free id, reused as soon as the bomb clears locally.
    private fun nextBombId(): Int {
        val taken = _live.map { it.bombNo }.toSet()
        return (0 until bombCount).firstOrNull { it !in taken } ?: 0
    }

    // Fires the server fuse for due bombs and applies RESPONSE_EXPLODE; no push still frees the slot.
    private fun deliverDueExplosions() {
        val now = clock.nowMs()
        val due = _live.filter { it.readyAtMs <= now }
        for (bomb in due) {
            _live.remove(bomb)
            val result = client.fireFuse(heroId, bomb.bombNo, bomb.cell)
            if (result.pushed) {
                explodesAccepted++
                for ((i, j, hp) in result.blocks) {
                    if (hp <= 0) clientMap.removeBrick(cell(i, j))
                }
            } else {
                explodesRejected++
                explodeRejections.add(bomb.bombNo to bomb.cell)
            }
        }
    }

    // Only reject a target unreachable for 3s straight.
    private fun onTargetUnreachable(target: Cell) {
        if (_unreachableTarget != target) {
            _unreachableTarget = target
            _unreachableSinceMs = clock.nowMs()
            return
        }
        if (clock.nowMs() - _unreachableSinceMs < UnreachableRejectDelayMs) return
        _unreachableSinceMs = clock.nowMs()
        targets.rejectTarget(heroId, target)
        rejectsSent++
    }

    // Ms per tile at this speed, same relation as the server's move-time check.
    private fun stepMs(): Long = (1000.0 / speed).toLong().coerceAtLeast(1)
}
