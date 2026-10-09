package com.senspark.mapservice.stresstest

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicLongArray
import kotlin.random.Random

/**
 * Per-endpoint counters + a reservoir-sampled latency distribution (fixed-size, random-replacement
 * so it stays representative over an arbitrarily long run without unbounded memory).
 */
class EndpointMetrics(reservoirSize: Int = 4096) {
    private val count = AtomicLong(0)
    private val errorCount = AtomicLong(0)
    private val sum = AtomicLong(0)
    private val min = AtomicLong(Long.MAX_VALUE)
    private val max = AtomicLong(0)
    private val reservoir = AtomicLongArray(reservoirSize)
    private val reservoirFill = AtomicLong(0)
    private val resultCounts = ConcurrentHashMap<String, AtomicLong>()

    fun recordSuccess(latencyMs: Long, resultTag: String? = null) {
        count.incrementAndGet()
        sum.addAndGet(latencyMs)
        min.updateAndGet { kotlin.math.min(it, latencyMs) }
        max.updateAndGet { kotlin.math.max(it, latencyMs) }
        offerReservoir(latencyMs)
        if (resultTag != null) {
            resultCounts.getOrPut(resultTag) { AtomicLong(0) }.incrementAndGet()
        }
    }

    fun recordError() {
        errorCount.incrementAndGet()
    }

    private fun offerReservoir(v: Long) {
        val n = reservoirFill.incrementAndGet()
        val size = reservoir.length()
        if (n <= size) {
            reservoir[(n - 1).toInt()] = v
        } else {
            val idx = Random.nextLong(n)
            if (idx < size) reservoir[idx.toInt()] = v
        }
    }

    fun snapshot(): Snapshot {
        val c = count.get()
        val filled = minOf(reservoirFill.get(), reservoir.length().toLong()).toInt()
        val samples = LongArray(filled) { reservoir[it] }
        samples.sort()
        fun pct(p: Double): Long = if (samples.isEmpty()) 0 else samples[((samples.size - 1) * p).toInt()]
        return Snapshot(
            count = c,
            errors = errorCount.get(),
            avgMs = if (c == 0L) 0.0 else sum.get().toDouble() / c,
            minMs = if (c == 0L) 0 else min.get(),
            maxMs = max.get(),
            p50 = pct(0.50),
            p95 = pct(0.95),
            p99 = pct(0.99),
            resultBreakdown = resultCounts.mapValues { it.value.get() },
        )
    }

    data class Snapshot(
        val count: Long,
        val errors: Long,
        val avgMs: Double,
        val minMs: Long,
        val maxMs: Long,
        val p50: Long,
        val p95: Long,
        val p99: Long,
        val resultBreakdown: Map<String, Long>,
    )
}

/** One [EndpointMetrics] per MapService route, keyed by short name ("init", "auto_start", "keepalive", ...). */
class Metrics {
    private val endpoints = ConcurrentHashMap<String, EndpointMetrics>()
    val mapsCleared = AtomicLong(0)
    private val startedAtNanos = System.nanoTime()

    fun endpoint(name: String): EndpointMetrics = endpoints.getOrPut(name) { EndpointMetrics() }

    fun report(): String {
        val elapsedS = (System.nanoTime() - startedAtNanos) / 1_000_000_000.0
        val sb = StringBuilder()
        sb.appendLine("=== MapService stress test report (t+%.1fs) ===".format(elapsedS))
        var totalReq = 0L
        var totalErr = 0L
        for (name in endpoints.keys.sorted()) {
            val s = endpoints.getValue(name).snapshot()
            totalReq += s.count
            totalErr += s.errors
            val rps = if (elapsedS > 0) s.count / elapsedS else 0.0
            sb.appendLine(
                "%-9s reqs=%-7d err=%-5d rps=%-8.1f avg=%-6.1fms p50=%-5dms p95=%-5dms p99=%-5dms max=%-6dms"
                    .format(name, s.count, s.errors, rps, s.avgMs, s.p50, s.p95, s.p99, s.maxMs)
            )
            if (s.resultBreakdown.isNotEmpty()) {
                sb.appendLine("          results: " + s.resultBreakdown.entries.sortedBy { it.key }.joinToString(", ") { "${it.key}=${it.value}" })
            }
        }
        if (mapsCleared.get() > 0) sb.appendLine("maps cleared: ${mapsCleared.get()}")
        val totalRps = if (elapsedS > 0) totalReq / elapsedS else 0.0
        sb.append("TOTAL     reqs=$totalReq err=$totalErr rps=%.1f".format(totalRps))
        return sb.toString()
    }
}
