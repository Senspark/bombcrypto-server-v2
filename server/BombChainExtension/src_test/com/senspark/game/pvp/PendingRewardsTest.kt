package com.senspark.game.pvp

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PendingRewardsTest {
    private class Reward(override val rewardId: String) : IPvpMatchReward {
        override val isOutOfChestSlot = false
    }

    @Test
    fun `claim that arrives before the result waits for it`() = runTest {
        val rewards = PendingRewards()
        val claim = async { rewards.take(1, 10_000) }
        delay(100)
        rewards.put(1, Reward("match:1"))
        assertEquals("match:1", claim.await()?.rewardId)
    }

    @Test
    fun `result that arrives first is kept for the claim`() = runTest {
        val rewards = PendingRewards()
        rewards.put(1, Reward("match:1"))
        assertEquals("match:1", rewards.take(1, 10_000)?.rewardId)
    }

    @Test
    fun `a reward is claimed only once`() = runTest {
        val rewards = PendingRewards()
        rewards.put(1, Reward("match:1"))
        rewards.take(1, 10_000)
        assertNull(rewards.take(1, 1_000))
    }

    @Test
    fun `claim without a result times out with null`() = runTest {
        val rewards = PendingRewards()
        assertNull(rewards.take(1, 1_000))
    }

    @Test
    fun `result after a timed out claim is kept for the next claim`() = runTest {
        val rewards = PendingRewards()
        assertNull(rewards.take(1, 1_000))
        rewards.put(1, Reward("match:1"))
        assertEquals("match:1", rewards.take(1, 1_000)?.rewardId)
    }

    @Test
    fun `rewards of different users do not mix`() = runTest {
        val rewards = PendingRewards()
        val claim = async { rewards.take(2, 10_000) }
        delay(100)
        rewards.put(1, Reward("match:1"))
        rewards.put(2, Reward("match:2"))
        assertEquals("match:2", claim.await()?.rewardId)
        assertEquals("match:1", rewards.take(1, 1_000)?.rewardId)
    }
}
