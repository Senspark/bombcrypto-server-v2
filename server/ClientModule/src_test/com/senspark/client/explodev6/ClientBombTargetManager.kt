package com.senspark.client.explodev6

import kotlin.math.min
import kotlin.math.pow

// Kotlin port of Unity's DefaultBombTargetManager (BombTargetManager.cs).
// Differences: injectable clock [nowMs], and [flush] is called explicitly instead of next frame.
class ClientBombTargetManager(
    private val client: FakeGameClient,
    private val nowMs: () -> Long,
) {
    companion object {
        const val InFlightExpiryMs = 10_000L
        const val NoTargetRetryCooldownMs = 3_000L
    }

    private val _targets = mutableMapOf<Int, Cell>()
    private var _pendingSeeds = mutableMapOf<Int, Cell>()
    private val _pendingRejects = mutableMapOf<Int, Cell>()
    private val _inFlight = mutableMapOf<Int, Long>()
    private val _noTargetRetryAt = mutableMapOf<Int, Long>()
    private val _rejectStreak = mutableMapOf<Int, Int>()

    val sentBatches = mutableListOf<Map<Int, Cell>>()

    fun tryGetTarget(heroId: Int): Cell? = _targets[heroId]

    fun hasTarget(heroId: Int): Boolean = _targets.containsKey(heroId)

    fun requestTarget(heroId: Int, seedHint: Cell) {
        if (_targets.containsKey(heroId)) return
        val since = _inFlight[heroId]
        if (since != null) {
            if (nowMs() - since < InFlightExpiryMs) return
            _inFlight.remove(heroId)
        }
        if (_pendingSeeds.containsKey(heroId)) return
        val retryAt = _noTargetRetryAt[heroId]
        if (retryAt != null && nowMs() < retryAt) return
        _pendingSeeds[heroId] = seedHint
    }

    fun rejectTarget(heroId: Int, cell: Cell) {
        if (_pendingRejects[heroId] == cell) return
        val streak = (_rejectStreak[heroId] ?: 0) + 1
        _rejectStreak[heroId] = streak
        _pendingRejects[heroId] = cell
        _targets.remove(heroId)
        val delaySeconds = min(NoTargetRetryCooldownMs / 1000.0, 0.25 * 2.0.pow(min(streak - 1, 8)))
        _noTargetRetryAt[heroId] = nowMs() + (delaySeconds * 1000).toLong()
    }

    // Next target comes back on the START_PLANT_BOMB response, so mark in-flight.
    fun notifyPlanted(heroId: Int) {
        _targets.remove(heroId)
        _inFlight[heroId] = nowMs()
        _rejectStreak.remove(heroId)
    }

    fun onBombTarget(heroId: Int, location: Cell) {
        _targets[heroId] = location
        _inFlight.remove(heroId)
    }

    fun onBombTargetMismatch(heroId: Int) {
        _targets.remove(heroId)
        _inFlight.remove(heroId)
    }

    fun onNewMap() {
        _targets.clear()
        _pendingSeeds.clear()
        _pendingRejects.clear()
        _inFlight.clear()
        _noTargetRetryAt.clear()
        _rejectStreak.clear()
    }

    // Returns true if a request was sent.
    fun flush(): Boolean {
        if (_pendingSeeds.isEmpty()) return false
        val batch = _pendingSeeds
        _pendingSeeds = mutableMapOf()

        val rejects = mutableMapOf<Int, Cell>()
        for (heroId in batch.keys) {
            _pendingRejects.remove(heroId)?.let { rejects[heroId] = it }
            _inFlight[heroId] = nowMs()
        }
        sentBatches.add(batch.toMap())
        try {
            val results = client.getBombTargets(batch, rejects)
            val resolved = mutableSetOf<Int>()
            for (result in results) {
                _targets[result.heroId] = result.location
                _noTargetRetryAt.remove(result.heroId)
                resolved.add(result.heroId)
            }
            for (heroId in batch.keys) {
                if (heroId !in resolved) {
                    _noTargetRetryAt[heroId] = nowMs() + NoTargetRetryCooldownMs
                }
            }
        } finally {
            for (heroId in batch.keys) {
                _inFlight.remove(heroId)
            }
        }
        return true
    }
}
