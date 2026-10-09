package com.senspark.mapservice.stresstest

import com.senspark.mapservice.model.AutoHeroDto
import com.senspark.mapservice.model.AutoHeroesRequest
import com.senspark.mapservice.model.AutoStartRequest
import com.senspark.mapservice.model.BlockDto
import com.senspark.mapservice.model.MapInitRequest
import com.senspark.mapservice.model.MapReplaceRequest
import com.senspark.mapservice.model.TreasureEventType
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random

private const val KEEPALIVE_INTERVAL_MS = 30_000L
private const val TEARDOWN_GRACE_MS = 500L
private const val MAX_PRINTED_VIOLATIONS = 5

// One player driven the way the game server drives a real user after START_TREASURE_MODE;
// every event is replayed through [TreasureReplay], so a broken stream shows up as `ev_invalid`.
class AutoVirtualUser(
    private val index: Int,
    private val client: MapServiceStressClient,
    private val tracker: TreasureEventTracker,
    private val metrics: Metrics,
    private val config: StressTestConfig,
) {
    private val random = Random(config.seed + index)
    private val dataType = StressData.dataTypes[index % StressData.dataTypes.size]
    private val sessionKey = "stress-auto-$index-$dataType-PVE_V2"
    private val rewardConfig = StressData.rewardConfigFor(dataType)

    private val heroes = (0 until config.heroesPerUser).map { h ->
        val heroId = index * 100_000 + h
        AutoHeroDto(
            hero = StressData.randomHero(heroId, dataType, random.nextDouble() < config.airdropRatio, config.heroRarity, random),
            speed = random.nextInt(1, 9),
            bombCount = random.nextInt(1, config.bombsPerHero + 1),
            blockPass = random.nextDouble() < config.blockPassRatio,
        )
    }
    private val removed = mutableSetOf<Int>()
    private var pendingBlocks: List<BlockDto>? = null
    private var printedViolations = 0

    suspend fun run(untilMs: Long) {
        val initialMap = StressData.randomMap(config.mapDensity, random)
        try {
            client.deleteSession(sessionKey)
        } catch (e: Exception) {
            // Best-effort cleanup of a previous run's session.
        }
        if (!retry(untilMs) { client.initSession(sessionKey, MapInitRequest(initialMap, 0, "PVE_V2", rewardConfig)) }) {
            System.err.println("[auto-user-$index] could not init session $sessionKey, giving up")
            return
        }
        val queue = tracker.register(sessionKey)
        val snapshot = client.autoStart(sessionKey, AutoStartRequest(heroes, config.fuseMs, config.mapResetPauseMs))
        if (snapshot == null) {
            System.err.println("[auto-user-$index] auto/start failed for $sessionKey, giving up")
            tracker.unregister(sessionKey)
            return
        }
        val replay = TreasureReplay(
            snapshot,
            blockPass = heroes.filter { it.blockPass }.map { it.hero.heroId }.toSet(),
            capacity = heroes.associate { it.hero.heroId to maxOf(1, it.bombCount) },
        )

        var nextKeepaliveAt = System.currentTimeMillis() + KEEPALIVE_INTERVAL_MS
        var nextChurnAt = nextChurnTime()
        while (System.currentTimeMillis() < untilMs) {
            try {
                if (drain(queue, replay)) pushFreshMap()
                val now = System.currentTimeMillis()
                if (now >= nextKeepaliveAt) {
                    client.autoKeepalive(sessionKey)
                    nextKeepaliveAt = now + KEEPALIVE_INTERVAL_MS
                }
                if (now >= nextChurnAt) {
                    churnRoster()
                    nextChurnAt = nextChurnTime()
                }
            } catch (e: Exception) {
                // Already recorded against the failing endpoint; keep this user alive.
            }
            delay(config.thinkTimeMs)
        }

        try {
            client.autoStop(sessionKey)
            client.deleteSession(sessionKey)
        } catch (e: Exception) {
            // Best-effort cleanup only.
        }
        delay(TEARDOWN_GRACE_MS)
        drain(queue, replay)
        tracker.unregister(sessionKey)
    }

    /** Replays everything received so far; true if a bomb just cleared the map (the server would push a new one). */
    private fun drain(queue: ConcurrentLinkedQueue<TreasureEventTracker.Received>, replay: TreasureReplay): Boolean {
        var mapCleared = false
        while (true) {
            val received = queue.poll() ?: break
            for (event in received.batch.events) {
                val errorsBefore = replay.errors.size
                replay.apply(event)
                if (event.type == TreasureEventType.NEW_MAP) {
                    pendingBlocks?.let { replay.loadBlocks(it) }
                    pendingBlocks = null
                    metrics.mapsCleared.incrementAndGet()
                }
                if (event.type == TreasureEventType.EXPLODE && event.mapNowEmpty == true) mapCleared = true
                metrics.endpoint("ev_${event.type}").recordSuccess((received.receivedAtMs - event.atMs).coerceAtLeast(0))
                if (replay.errors.size > errorsBefore) {
                    metrics.endpoint("ev_invalid").recordError()
                    if (printedViolations++ < MAX_PRINTED_VIOLATIONS) {
                        System.err.println("[auto-user-$index] invalid stream: ${replay.errors.last()}")
                    }
                }
            }
        }
        return mapCleared
    }

    private suspend fun pushFreshMap() {
        val blocks = StressData.randomMap(config.mapDensity, random)
        pendingBlocks = blocks
        client.replaceMap(sessionKey, MapReplaceRequest(blocks, 0, "PVE_V2"))
    }

    // Mirrors a player sending a hero home (or it running out of energy) and later back to work.
    private suspend fun churnRoster() {
        if (removed.isNotEmpty() && random.nextBoolean()) {
            val back = removed.random(random)
            removed.remove(back)
            client.autoHeroes(sessionKey, AutoHeroesRequest(upsert = heroes.filter { it.hero.heroId == back }))
        } else {
            val active = heroes.map { it.hero.heroId }.filter { it !in removed }
            if (active.size <= 1) return
            val out = active.random(random)
            removed.add(out)
            client.autoHeroes(sessionKey, AutoHeroesRequest(remove = listOf(out), reason = "churn"))
        }
    }

    private fun nextChurnTime(): Long {
        if (config.churnPerMinute <= 0.0) return Long.MAX_VALUE
        val meanMs = 60_000.0 / config.churnPerMinute
        return System.currentTimeMillis() + (meanMs * (0.5 + random.nextDouble())).toLong()
    }

    private suspend fun retry(untilMs: Long, block: suspend () -> Boolean): Boolean {
        repeat(5) {
            if (System.currentTimeMillis() >= untilMs) return false
            try {
                if (block()) return true
            } catch (e: Exception) {
                // Recorded by the client wrapper.
            }
            delay(500)
        }
        return false
    }
}
