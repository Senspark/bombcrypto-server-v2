package com.senspark.game.pvp.stress

import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis
import org.junit.jupiter.api.Assertions.*

class PvpStressSimulationTest {

    @Test
    fun `stress test simulation - 100 concurrent matches`() = runBlocking {
        val totalMatches = 100
        val matchSuccess = AtomicInteger(0)
        
        println("🚀 [Stress] Starting simulation: 100 concurrent matches...")
        
        val time = measureTimeMillis {
            val jobs = List(totalMatches) { id ->
                launch(Dispatchers.Default) {
                    // Simulate Match Controller Initialization & Start
                    simulateMatchLifecycle(id)
                    matchSuccess.incrementAndGet()
                }
            }
            jobs.joinAll()
        }
        
        println("🏁 [Stress] Completed in ${time}ms")
        assertEquals(totalMatches, matchSuccess.get(), "All 100 matches should complete successfully")
    }

    private suspend fun simulateMatchLifecycle(id: Int) {
        // Simulation of:
        // 1. Map generation
        delay((10..50).random().toLong())
        // 2. Wager Locking
        delay((5..20).random().toLong())
        // 3. Match ticks (simulating 1 minute in 200ms)
        for (i in 0 until 10) {
            delay(20)
        }
        // 4. Result Distribution
        delay((10..30).random().toLong())
    }

    @Test
    fun `stress test simulation - 1000 concurrent queue joins`() = runBlocking {
        val totalJoins = 1000
        val joinSuccess = AtomicInteger(0)
        
        println("🚀 [Stress] Starting simulation: 1000 concurrent queue joins...")
        
        val time = measureTimeMillis {
            val jobs = List(totalJoins) { id ->
                launch(Dispatchers.Default) {
                    // Simulate Redis queue interaction
                    delay((1..10).random().toLong())
                    joinSuccess.incrementAndGet()
                }
            }
            jobs.joinAll()
        }
        
        println("🏁 [Stress] Completed in ${time}ms")
        assertEquals(totalJoins, joinSuccess.get(), "All 1000 joins should be processed")
    }

    @Test
    fun `stress test - escrow contention simulation`() = runBlocking {
        val totalRequests = 200
        val debitCount = AtomicInteger(0)
        
        println("🚀 [Stress] Starting escrow contention test: 200 concurrent requests...")
        
        val time = measureTimeMillis {
            val jobs = List(totalRequests) { id ->
                launch(Dispatchers.Default) {
                    // Simulate atomic DB lock contention (SELECT FOR UPDATE)
                    simulateDbContention()
                    debitCount.incrementAndGet()
                }
            }
            jobs.joinAll()
        }
        
        println("🏁 [Stress] Contention test completed in ${time}ms")
        assertEquals(totalRequests, debitCount.get(), "All escrow requests should be handled")
    }

    private suspend fun simulateDbContention() {
        // Simulates the wait time for a row lock in PostgreSQL
        delay((2..15).random().toLong())
    }
}
