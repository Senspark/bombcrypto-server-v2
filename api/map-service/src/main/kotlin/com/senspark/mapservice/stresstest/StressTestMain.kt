package com.senspark.mapservice.stresstest

import com.senspark.mapservice.model.ConfigUpdateRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Entry point for the MapService stress-test client (see README § Stress testing). Spins up
 * `config.users` [AutoVirtualUser]s concurrently, each with its own session key and hero roster, all
 * driving requests straight at MapService -- never through bombcrypto-server-v2. Run via:
 *
 * ```
 * ./gradlew stressTest -Pargs="--users 10 --duration 60"
 * ```
 */
fun main(args: Array<String>) = runBlocking {
    val config = StressTestConfig.parse(args)

    // Tee everything this run prints to stdout into a timestamped file under --report-dir, so the
    // live log survives after the terminal scrolls away. Opened up front so the startup line and
    // every periodic/final report all land in the same file.
    val reportFile = if (config.reportDir.isNotBlank()) {
        val dir = File(config.reportDir).apply { mkdirs() }
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        File(dir, "stress-test-$timestamp.log")
    } else {
        null
    }
    val reportWriter = reportFile?.bufferedWriter()
    fun log(line: String) {
        println(line)
        reportWriter?.let {
            it.write(line)
            it.newLine()
            it.flush()
        }
    }

    log(
        "MapService stress test: users=${config.users} heroesPerUser=${config.heroesPerUser} " +
            "duration=${config.durationSeconds}s baseUrl=${config.baseUrl} redisUrl=${config.redisConnectionString} " +
            "seed=${config.seed}"
    )

    // Java engine reuses keep-alive connections (bounded by in-flight requests, ~1 per user since each
    // VirtualUser calls sequentially). Ktor 2's CIO engine opened a new socket per request, so every
    // call left a TIME_WAIT behind and the run died with BindException once ~16k local ports ran out.
    val http = HttpClient(Java) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 5_000
        }
        engine {
            protocolVersion = java.net.http.HttpClient.Version.HTTP_1_1
        }
    }

    val metrics = Metrics()
    val client = MapServiceStressClient(http, config.baseUrl, metrics)
    val treasureTracker = TreasureEventTracker(config.redisConnectionString, metrics)
    treasureTracker.start()

    // Mirrors the main server's own POST /config on a reward-config reload; each session's
    // /init below also carries its own copy, same as UserBlockMapManagerV2.buildInitRequest does.
    try {
        client.pushConfig(ConfigUpdateRequest(rewardConfig = StressData.rewardConfigFor(StressData.dataTypes.first())))
    } catch (e: Exception) {
        System.err.println("Warning: initial POST /config failed (${e.message}); continuing anyway")
    }

    val startMs = System.currentTimeMillis()
    val endMs = startMs + config.durationSeconds * 1000L

    val reporter = launch {
        while (isActive) {
            delay(config.printIntervalSeconds * 1000L)
            log(metrics.report())
        }
    }

    val userJobs = (0 until config.users).map { idx ->
        launch {
            if (config.rampUpSeconds > 0 && config.users > 1) {
                delay((config.rampUpSeconds * 1000L * idx) / config.users)
            }
            AutoVirtualUser(idx, client, treasureTracker, metrics, config).run(endMs)
        }
    }

    userJobs.joinAll()
    reporter.cancel()
    treasureTracker.stop()

    log(metrics.report())
    log("Stress test finished in %.1fs".format((System.currentTimeMillis() - startMs) / 1000.0))
    http.close()
    reportWriter?.close()
    if (reportFile != null) println("Report saved to ${reportFile.path}")
}
