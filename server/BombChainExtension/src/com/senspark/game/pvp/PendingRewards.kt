package com.senspark.game.pvp

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The match result reaches this server over Redis, the client's claim arrives on its own; either can come first.
 */
class PendingRewards {
    private val _locker = Any()
    private val _rewards = mutableMapOf<Int, IPvpMatchReward>()
    private val _waiters = mutableMapOf<Int, CompletableDeferred<IPvpMatchReward>>()

    fun put(userId: Int, reward: IPvpMatchReward) {
        synchronized(_locker) {
            val waiter = _waiters.remove(userId)
            if (waiter != null) {
                waiter.complete(reward)
            } else {
                _rewards[userId] = reward
            }
        }
    }

    suspend fun take(userId: Int, timeoutMs: Long): IPvpMatchReward? {
        val waiter = synchronized(_locker) {
            _rewards.remove(userId)?.let { return it }
            CompletableDeferred<IPvpMatchReward>().also { _waiters[userId] = it }
        }
        return withTimeoutOrNull(timeoutMs) { waiter.await() } ?: synchronized(_locker) {
            if (_waiters[userId] === waiter) {
                _waiters.remove(userId)
            }
            // put() may have completed the waiter right as the timeout fired.
            waiter.getCompletedOrNull()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun CompletableDeferred<IPvpMatchReward>.getCompletedOrNull() =
        if (isCompleted) getCompleted() else null
}
