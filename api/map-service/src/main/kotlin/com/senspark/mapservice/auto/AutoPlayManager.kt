package com.senspark.mapservice.auto

import com.senspark.mapservice.domain.Session
import com.senspark.mapservice.domain.SessionStore
import com.senspark.mapservice.model.AutoHeroDto
import com.senspark.mapservice.model.AutoSnapshotDto
import com.senspark.mapservice.model.TreasureEventBatch
import com.senspark.mapservice.redis.TreasureEventPublisher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.withLock
import kotlin.random.Random

// One runner coroutine per auto-playing session: advance to now, publish the batch, sleep until the next
// due action or a signal. All publishing for a session goes through it, so the stream stays in seq order.
class AutoPlayManager(
    private val store: SessionStore,
    private val publisher: TreasureEventPublisher,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    private val randomFor: (String) -> Random = { Random.Default },
    // No request for this long = the game server is gone and nobody credits the explosions: the game stops.
    private val leaseMs: Long = DEFAULT_LEASE_MS,
) {
    companion object {
        // Three missed 30 s keepalives plus slack.
        const val DEFAULT_LEASE_MS = 100_000L
        private val log = LoggerFactory.getLogger(AutoPlayManager::class.java)
    }

    private class Runner(val session: Session, val wake: Channel<Unit>, var job: Job? = null)

    private val runners = ConcurrentHashMap<String, Runner>()

    fun start(key: String, session: Session, heroes: List<AutoHeroDto>, config: AutoConfig, paused: Boolean = false): AutoSnapshotDto {
        return session.lock.withLock {
            val now = clock()
            val auto = session.autoPlay ?: AutoPlay(session, config, randomFor(key)).also { session.autoPlay = it }
            // Due actions run at their own time first, so event times never go backwards.
            auto.advance(now)
            val snapshot = auto.start(heroes, config, now, paused)
            auto.lastSeenAt = now
            ensureRunner(key, session)
            snapshot
        }
    }

    /** Runs [block] against a running session's [AutoPlay]; false if auto mode isn't running. */
    fun update(key: String, session: Session, block: AutoPlay.(now: Long) -> Unit): Boolean {
        val applied = session.lock.withLock {
            val auto = session.autoPlay ?: return false
            if (!auto.running) return false
            val now = clock()
            auto.lastSeenAt = now
            auto.advance(now)
            auto.block(now)
            true
        }
        signal(key)
        return applied
    }

    // The game server's keepalive: renews the lease of a running game.
    fun keepalive(session: Session) {
        session.lock.withLock { session.autoPlay?.lastSeenAt = clock() }
    }

    fun snapshot(session: Session): AutoSnapshotDto? = session.lock.withLock { session.autoPlay?.snapshot(clock()) }

    // Wakes the runner after a change made outside it (map replace, roster edit, stop).
    fun signal(key: String) {
        runners[key]?.wake?.trySend(Unit)
    }

    fun cancel(key: String) {
        runners.remove(key)?.job?.cancel()
    }

    fun isRunnerActive(key: String): Boolean = runners[key]?.job?.isActive == true

    // Caller holds session.lock, which also guards the runner's own decision to exit.
    private fun ensureRunner(key: String, session: Session) {
        val existing = runners[key]
        if (existing != null && existing.session === session && existing.job?.isActive == true) {
            existing.wake.trySend(Unit)
            return
        }
        existing?.job?.cancel()
        val runner = Runner(session, Channel(Channel.CONFLATED))
        runners[key] = runner
        runner.job = scope.launch { run(key, runner) }
    }

    private suspend fun run(key: String, runner: Runner) {
        val session = runner.session
        try {
            while (scope.isActive) {
                if (store.get(key) !== session) break
                var finished = false
                val (events, nextDue) = try {
                    session.lock.withLock {
                        val auto = session.autoPlay ?: return
                        val now = clock()
                        var next = auto.advance(now)
                        if (auto.running && now - auto.lastSeenAt >= leaseMs) {
                            log.warn("auto play lease expired session=$key idleMs=${now - auto.lastSeenAt}, stopping")
                            auto.stop(now)
                            next = auto.advance(now)
                        }
                        finished = auto.isFinished
                        // Also wake up when the lease runs out, even with nothing else due.
                        if (auto.running) next = minOf(next ?: Long.MAX_VALUE, auto.lastSeenAt + leaseMs)
                        auto.drainEvents() to next
                    }
                } catch (e: Exception) {
                    // Drop the broken game: keepalive then reports running=false and the game server resyncs.
                    log.error("auto play failed session=$key, discarding its state", e)
                    session.lock.withLock { session.autoPlay = null }
                    return
                }
                if (events.isNotEmpty()) publish(key, TreasureEventBatch(key, events))
                if (finished) {
                    val done = session.lock.withLock {
                        (session.autoPlay?.isFinished != false).also { if (it) runners.remove(key, runner) }
                    }
                    if (done) return
                    continue
                }
                val waitMs = nextDue?.let { it - clock() }
                when {
                    waitMs == null -> runner.wake.receive()
                    waitMs > 0 -> withTimeoutOrNull(waitMs) { runner.wake.receive() }
                }
            }
        } finally {
            runners.remove(key, runner)
        }
    }

    private fun publish(key: String, batch: TreasureEventBatch) {
        try {
            publisher.publish(batch)
        } catch (e: Exception) {
            log.error("treasure event publish failed session=$key events=${batch.events.size} seq=${batch.events.first().seq}..${batch.events.last().seq}", e)
        }
    }
}
