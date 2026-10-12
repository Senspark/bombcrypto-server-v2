package com.senspark.mapservice.auto

import com.senspark.mapservice.domain.GameConstants
import com.senspark.mapservice.domain.Session
import com.senspark.mapservice.model.AutoBombDto
import com.senspark.mapservice.model.AutoHeroDto
import com.senspark.mapservice.model.AutoSnapshotDto
import com.senspark.mapservice.model.BlockDto
import com.senspark.mapservice.model.CellDto
import com.senspark.mapservice.model.HeroPositionDto
import com.senspark.mapservice.model.HeroSnapshotDto
import com.senspark.mapservice.model.TreasureEventDto
import com.senspark.mapservice.model.TreasureEventType
import com.senspark.mapservice.model.toDto
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.random.Random

data class AutoConfig(
    val fuseMs: Long = 3000,
    val mapResetPauseMs: Long = AutoPlay.DEFAULT_MAP_RESET_PAUSE_MS,
)

internal class AutoHero(var dto: AutoHeroDto, var pos: Cell) {
    val heroId: Int get() = dto.hero.heroId
    val stepMs: Long get() = (1000.0 / max(dto.speed, 1)).roundToLong()
    val bombCapacity: Int get() = max(dto.bombCount, 1)

    var target: Cell? = null

    // Tiles still to walk; the first one is the tile the hero is stepping onto right now.
    val path = ArrayDeque<Cell>()
    var pathVersion = -1L
    var announcedStepMs = 0L
    var nextActionAt = 0L

    // BotManager.notPathToLocationList: targets this hero gave up on; cleared whenever it would outgrow its size.
    val noPath = HashSet<Cell>()

    fun giveUpOn(cell: Cell) {
        if (noPath.size >= AutoPlay.NO_PATH_MEMORY) noPath.clear()
        noPath.add(cell)
    }

    // Set while the client walks this hero around on its own: 0 = open-ended, else the time it is needed back.
    var roamUntil: Long? = null
}

internal class AutoBomb(
    val heroId: Int,
    val bombNo: Int,
    val cell: Cell,
    val plantedAt: Long,
    val explodeAt: Long,
    val snapshot: HeroSnapshotDto,
)

// Server-driven treasure mode for one session: walks, plants and explodes like the client's BotManager did,
// recording each step as an ordered event. Clock-free: callers pass `now` and hold [Session.lock].
class AutoPlay(
    private val session: Session,
    config: AutoConfig = AutoConfig(),
    private val random: Random = Random.Default,
) {
    companion object {
        const val DEFAULT_MAP_RESET_PAUSE_MS = 3000L
        const val NO_TARGET_RETRY_MS = 1000L
        const val UNREACHABLE_RETRY_MS = 250L
        const val NO_PATH_MEMORY = 10

        // Enough to plant and then try every target the no-path memory can cycle through.
        private const val MAX_DECISIONS_PER_ACTION = NO_PATH_MEMORY + 4
        private const val MAX_STEPS_PER_ADVANCE = 200_000
        private const val NEVER = Long.MAX_VALUE
    }

    var config: AutoConfig = config
        private set
    var running = false
        private set
    var seq = 0L
        private set
    var awaitingNewMap = false
        private set
    private var pausedUntil = 0L

    // Player paused: heroes halt at their next tile; bombs already planted still explode.
    var paused = false
        private set

    // Last request from the game server; AutoPlayManager stops a game it no longer hears from.
    var lastSeenAt = 0L

    // Tests pin spawn cells; production spawns like the client's TakeEmptyLocations.
    internal var spawnOverride: ((Int) -> List<Cell>)? = null

    private val heroes = LinkedHashMap<Int, AutoHero>()

    // Per hero for the whole session, so a hero that leaves and rejoins never reuses a live bomb's number.
    private val nextBombNo = HashMap<Int, Int>()
    private val bombs = ArrayList<AutoBomb>()
    private val outbox = ArrayList<TreasureEventDto>()

    val heroIds: Set<Int> get() = heroes.keys.toSet()
    val liveBombCount: Int get() = bombs.size

    // Stopped and every planted bomb has gone off: nothing left to simulate.
    val isFinished: Boolean get() = !running && bombs.isEmpty()

    // ================== roster / lifecycle ==================

    /** (Re)starts the simulation; a restart keeps hero positions and live bombs (resync). */
    fun start(dtos: List<AutoHeroDto>, config: AutoConfig, now: Long, paused: Boolean = false): AutoSnapshotDto {
        this.config = config
        running = true
        setPaused(paused, now)
        val wanted = dtos.associateBy { it.hero.heroId }
        for (id in heroes.keys.filter { it !in wanted }) {
            heroes.remove(id)
            session.forgetHero(id)
        }
        val newcomers = wanted.values.filter { it.hero.heroId !in heroes }
        val spawns = spawnCells(newcomers.size)
        newcomers.forEachIndexed { k, dto -> heroes[dto.hero.heroId] = AutoHero(dto, spawns[k]) }
        for (dto in wanted.values) {
            val hero = heroes.getValue(dto.hero.heroId)
            hero.dto = dto
            hero.path.clear()
            hero.target = null
            hero.noPath.clear()
            hero.roamUntil = null
            session.releaseAutoTarget(hero.heroId)
            hero.nextActionAt = resumeTime(now)
        }
        return snapshot(now)
    }

    fun upsertHeroes(dtos: List<AutoHeroDto>, now: Long) {
        for (dto in dtos) {
            val existing = heroes[dto.hero.heroId]
            if (existing != null) {
                // New stats apply from the next tile; a changed speed re-announces the path.
                existing.dto = dto
                continue
            }
            val hero = AutoHero(dto, spawnCells(1).first())
            hero.nextActionAt = resumeTime(now)
            heroes[hero.heroId] = hero
            emit(TreasureEventDto(0, TreasureEventType.HERO_JOIN, now, hero.heroId, hero.pos.first, hero.pos.second))
        }
    }

    // The hero's live bombs still explode; only the hero stops.
    fun removeHeroes(ids: Collection<Int>, reason: String, now: Long) {
        for (id in ids) {
            val hero = heroes.remove(id) ?: continue
            session.forgetHero(id)
            emit(
                TreasureEventDto(
                    0, TreasureEventType.HERO_LEAVE, now, id, hero.pos.first, hero.pos.second, reason = reason,
                )
            )
        }
    }

    fun stop(now: Long) {
        removeHeroes(heroes.keys.toList(), "stopped", now)
        running = false
    }

    fun setPaused(value: Boolean, now: Long) {
        if (paused == value) return
        paused = value
        if (value) return
        for (hero in heroes.values) {
            if (hero.nextActionAt != NEVER) continue
            hero.noPath.clear()
            hero.nextActionAt = resumeTime(now)
        }
    }

    /** Called by [Session.replaceMap] after it reset its own per-hero tracking. */
    fun onMapReplaced(now: Long) {
        bombs.clear()
        awaitingNewMap = false
        if (!running) return
        pausedUntil = now + config.mapResetPauseMs
        val spawns = spawnCells(heroes.size)
        heroes.values.forEachIndexed { k, hero ->
            hero.pos = spawns[k]
            hero.path.clear()
            hero.target = null
            hero.noPath.clear()
            hero.roamUntil = null
            hero.nextActionAt = pausedUntil
        }
        emit(
            TreasureEventDto(
                0, TreasureEventType.NEW_MAP, now,
                heroes = positions(),
                resumeAtMs = pausedUntil,
            )
        )
    }

    fun snapshot(now: Long) = AutoSnapshotDto(
        seq = seq,
        serverTimeMs = now,
        fuseMs = config.fuseMs,
        heroes = positions(),
        bombs = bombs.map { AutoBombDto(it.heroId, it.bombNo, it.cell.first, it.cell.second, it.plantedAt, it.explodeAt) },
        blocks = session.map.blocks.map { BlockDto(it.i, it.j, it.type, it.hp, it.maxHp) },
        awaitingNewMap = awaitingNewMap,
        resumeAtMs = pausedUntil,
        paused = paused,
    )

    fun drainEvents(): List<TreasureEventDto> {
        if (outbox.isEmpty()) return emptyList()
        val events = ArrayList(outbox)
        outbox.clear()
        return events
    }

    // ================== simulation ==================

    // Runs everything due by [now] at its own scheduled time, in time order (a detonation wins a tie).
    // Returns when the next action is due, or null.
    fun advance(now: Long): Long? {
        var steps = 0
        while (steps++ < MAX_STEPS_PER_ADVANCE) {
            val bomb = bombs.minByOrNull { it.explodeAt }
            val hero = heroes.values.minByOrNull { it.nextActionAt }
            val bombAt = bomb?.explodeAt ?: NEVER
            val heroAt = hero?.nextActionAt ?: NEVER
            if (bombAt > now && heroAt > now) break
            if (bomb != null && bombAt <= heroAt) explode(bomb, bombAt) else act(hero!!, heroAt)
        }
        val next = minOf(bombs.minOfOrNull { it.explodeAt } ?: NEVER, heroes.values.minOfOrNull { it.nextActionAt } ?: NEVER)
        return if (next == NEVER) null else next
    }

    private fun act(hero: AutoHero, t: Long) {
        if (hero.path.isNotEmpty()) {
            hero.pos = hero.path.removeFirst()
        }
        decide(hero, t)
    }

    // One tile-center decision, same order as BotManager.OnUpdate: target -> plant here or walk on.
    private fun decide(hero: AutoHero, t: Long) {
        if (awaitingNewMap || paused) {
            halt(hero, t)
            hero.nextActionAt = NEVER
            return
        }
        if (t < pausedUntil) {
            halt(hero, t)
            hero.nextActionAt = pausedUntil
            return
        }
        // Cells another hero stands on as its own target: it plants (or re-plants) there, nobody else comes.
        val occupied = heroes.values.filter { it !== hero && it.target == it.pos }.mapTo(HashSet()) { it.pos }
        for (attempt in 0 until MAX_DECISIONS_PER_ACTION) {
            var target = hero.target
            if (target != null && (!isLegalTarget(target) || target in occupied)) {
                // Its last brick was destroyed, or another hero took the cell: pick again now instead of walking there.
                session.releaseAutoTarget(hero.heroId)
                hero.target = null
                target = null
            }
            if (target == null) {
                target = pickTarget(hero, occupied)
                if (target == null) {
                    hero.noPath.clear()
                    roam(hero, t)
                    hero.nextActionAt = t + NO_TARGET_RETRY_MS
                    return
                }
                hero.target = target
                session.holdAutoTarget(hero.heroId, target)
            }

            if (hero.pos == target) {
                val blocking = bombs.filter { it.cell == target }.minByOrNull { it.explodeAt }
                if (blocking != null) {
                    // Camping next to the last brick: the hero stands on its bomb and re-plants when it explodes.
                    halt(hero, t)
                    hero.nextActionAt = blocking.explodeAt
                    return
                }
                val own = bombs.filter { it.heroId == hero.heroId }
                if (own.size >= hero.bombCapacity) {
                    // Out of bombs on a new cell: the client roams the hero and has it back here to plant.
                    val freeAt = own.minOf { it.explodeAt }
                    roam(hero, t, freeAt)
                    hero.nextActionAt = freeAt
                    return
                }
                // May be a tile mid-path that just became the target: stop here first.
                if (hero.path.isNotEmpty()) halt(hero, t)
                plant(hero, t)
                // Like BotManager.SpawnBomb: only on the last brick the hero stays and re-plants here,
                // unless a spare bomb can go on another side of it before this one explodes.
                if (session.map.blocks.size == 1) {
                    val next = otherSideOfLastBrick(hero, occupied) ?: hero.pos
                    hero.target = next
                    session.holdAutoTarget(hero.heroId, next)
                }
                continue
            }

            if (walkTowards(hero, target, t)) {
                hero.nextActionAt = t + hero.stepMs
                return
            }
            // Like BotMove.MoveToTargetLocation: no path, so give up on it and pick again right away.
            hero.giveUpOn(target)
            session.releaseAutoTarget(hero.heroId)
            hero.target = null
        }
        roam(hero, t)
        hero.nextActionAt = t + UNREACHABLE_RETRY_MS
    }

    // Port of BotManager.ChooseNextTarget + BotDestroyBrick.FindRandomTileNearBrick: the brick-adjacent free
    // tile closest in a straight line (ties at random), whether or not it can be walked to.
    private fun pickTarget(hero: AutoHero, occupied: Set<Cell>): Cell? {
        val map = session.map
        val brick = map.blockGrid()
        val bombCells = bombs.mapTo(HashSet()) { it.cell }
        val candidates = ArrayList<Cell>()
        for (i in 0 until GameConstants.MAP_MAX_COL) for (j in 0 until GameConstants.MAP_MAX_ROW) {
            val cell = i to j
            if (map.isWall(i, j) || brick[i][j] || cell in bombCells || cell in occupied || cell in hero.noPath) continue
            val nearBrick = brick.getOrNull(i - 1)?.get(j) == true || brick.getOrNull(i + 1)?.get(j) == true ||
                brick[i].getOrNull(j - 1) == true || brick[i].getOrNull(j + 1) == true
            if (nearBrick) candidates.add(cell)
        }
        // Its own tile only when there is nowhere else (the client's fallback to the first safe location).
        val pool = candidates.filter { it != hero.pos }.ifEmpty { candidates }
        if (pool.isEmpty()) return null
        fun distance(c: Cell) = (c.first - hero.pos.first).let { it * it } + (c.second - hero.pos.second).let { it * it }
        val nearest = pool.minOf { distance(it) }
        return pool.filter { distance(it) == nearest }.random(random)
    }

    // A hero with a bomb to spare on the last brick: the closest other free side of it that it reaches before
    // the fuse runs out, so it plants there instead of waiting for its bomb. Null = stay and re-plant here.
    private fun otherSideOfLastBrick(hero: AutoHero, occupied: Set<Cell>): Cell? {
        if (bombs.count { it.heroId == hero.heroId } >= hero.bombCapacity) return null
        val map = session.map
        val brick = map.blockGrid()
        val bombCells = bombs.mapTo(HashSet()) { it.cell }
        val taken = heroes.values.filter { it !== hero }.mapNotNullTo(HashSet()) { it.target }
        val passable = passableFor(hero)
        val maxSteps = (config.fuseMs - 1) / hero.stepMs
        var best: Cell? = null
        var bestSteps = Int.MAX_VALUE
        for (i in 0 until GameConstants.MAP_MAX_COL) for (j in 0 until GameConstants.MAP_MAX_ROW) {
            val cell = i to j
            if (cell == hero.pos || map.isWall(i, j) || brick[i][j] || cell in bombCells || cell in occupied || cell in taken) continue
            if (!map.hasBlockAround(i, j)) continue
            val steps = Pathfinder.shortestPath(passable, hero.pos, cell)?.size ?: continue
            if (steps <= maxSteps && steps < bestSteps) {
                best = cell
                bestSteps = steps
            }
        }
        return best
    }

    private fun isLegalTarget(cell: Cell) =
        session.map.canSetBoom(cell.first, cell.second) && session.map.hasBlockAround(cell.first, cell.second)

    /** Keeps the current path while it's still walkable, otherwise re-plans (emitting MOVE). False = unreachable. */
    private fun walkTowards(hero: AutoHero, target: Cell, t: Long): Boolean {
        val passable = passableFor(hero)
        val remaining = hero.path
        if (remaining.isNotEmpty() && remaining.last() == target && hero.announcedStepMs == hero.stepMs &&
            (hero.pathVersion == session.version || remaining.all { passable(it.first, it.second) })
        ) {
            hero.pathVersion = session.version
            return true
        }
        val path = Pathfinder.shortestPath(passable, hero.pos, target) ?: return false
        hero.path.clear()
        hero.path.addAll(path)
        hero.pathVersion = session.version
        hero.announcedStepMs = hero.stepMs
        hero.roamUntil = null
        emit(
            TreasureEventDto(
                0, TreasureEventType.MOVE, t, hero.heroId, hero.pos.first, hero.pos.second,
                path = path.map { CellDto(it.first, it.second) },
                stepMs = hero.stepMs,
            )
        )
        return true
    }

    private fun passableFor(hero: AutoHero): (Int, Int) -> Boolean {
        val map = session.map
        val blocked = map.blockGrid()
        val bombCells = bombs.mapTo(HashSet()) { it.cell }
        val blockPass = hero.dto.blockPass
        return { i, j -> !map.isWall(i, j) && (blockPass || !blocked[i][j]) && (i to j) !in bombCells }
    }

    // Stops a walking or roaming hero on its tile; an empty-path MOVE tells the client to stand here.
    private fun halt(hero: AutoHero, t: Long) {
        if (hero.path.isEmpty() && hero.roamUntil == null) return
        hero.path.clear()
        hero.pathVersion = -1
        hero.roamUntil = null
        emit(
            TreasureEventDto(
                0, TreasureEventType.MOVE, t, hero.heroId, hero.pos.first, hero.pos.second,
                path = emptyList(), stepMs = hero.stepMs,
            )
        )
    }

    // Nothing to do on this tile until [until] (0 = unknown): the client moves the hero around by itself.
    // The hero stays here for the simulation; announced once per wait.
    private fun roam(hero: AutoHero, t: Long, until: Long = 0) {
        if (hero.path.isEmpty() && hero.roamUntil == until) return
        hero.path.clear()
        hero.pathVersion = -1
        hero.roamUntil = until
        emit(
            TreasureEventDto(
                0, TreasureEventType.MOVE, t, hero.heroId, hero.pos.first, hero.pos.second,
                path = emptyList(), stepMs = hero.stepMs, roamUntilMs = until,
            )
        )
    }

    private fun plant(hero: AutoHero, t: Long) {
        val bombNo = nextBombNo.getOrDefault(hero.heroId, 0)
        nextBombNo[hero.heroId] = bombNo + 1
        val explodeAt = t + config.fuseMs
        session.recordAutoPlant(hero.heroId, bombNo, hero.pos, t)
        bombs.add(AutoBomb(hero.heroId, bombNo, hero.pos, t, explodeAt, hero.dto.hero))
        hero.target = null
        hero.roamUntil = null
        emit(
            TreasureEventDto(
                0, TreasureEventType.PLANT, t, hero.heroId, hero.pos.first, hero.pos.second,
                bombNo = bombNo, plantedAtMs = t, explodeAtMs = explodeAt,
            )
        )
    }

    private fun explode(bomb: AutoBomb, t: Long) {
        bombs.remove(bomb)
        val outcome = session.explode(bomb.bombNo, bomb.cell.first, bomb.cell.second, bomb.snapshot)
        emit(
            TreasureEventDto(
                0, TreasureEventType.EXPLODE, t, bomb.heroId, bomb.cell.first, bomb.cell.second,
                bombNo = bomb.bombNo,
                plantedAtMs = bomb.plantedAt,
                explodeAtMs = t,
                takeResult = outcome.takeResult.name,
                blocksHit = outcome.blocksHit.map { it.toDto() },
                mapNowEmpty = outcome.mapNowEmpty,
            )
        )
        if (outcome.mapNowEmpty && running) {
            // Heroes mid-step stop at their next tile (decide); the game server pushes a new map.
            awaitingNewMap = true
            for (hero in heroes.values) {
                if (hero.path.isEmpty()) hero.nextActionAt = NEVER
            }
        }
    }

    // ================== helpers ==================

    private fun resumeTime(now: Long) = if (awaitingNewMap || paused) NEVER else max(now, pausedUntil)

    private fun positions() = heroes.values.map { HeroPositionDto(it.heroId, it.pos.first, it.pos.second) }

    // Random empty cells, like the client's TakeEmptyLocations (distinct while enough exist).
    private fun spawnCells(count: Int): List<Cell> {
        if (count <= 0) return emptyList()
        spawnOverride?.let { return it(count) }
        val bombCells = bombs.mapTo(HashSet()) { it.cell }
        val cells = session.map.emptyCells().filter { it !in bombCells }.shuffled(random)
        if (cells.isEmpty()) return List(count) { 0 to 0 }
        return List(count) { cells[it % cells.size] }
    }

    private fun emit(event: TreasureEventDto) {
        outbox.add(event.copy(seq = ++seq))
    }
}
