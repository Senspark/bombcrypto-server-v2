package com.senspark.mapservice.stresstest

import kotlin.system.exitProcess

// Target MapService: edit this, or override per run with $MAP_SERVICE_URL or --base-url.
const val DEFAULT_BASE_URL = "http://localhost:8091"

data class StressTestConfig(
    val baseUrl: String = System.getenv("MAP_SERVICE_URL") ?: DEFAULT_BASE_URL,
    // Same env var name (and default) MapService's own mapServiceModule() reads -- the Redis this
    // run's TreasureEventTracker reads events from must be the same one MapService publishes to.
    val redisConnectionString: String = System.getenv("REDIS_CONNECTION_STRING") ?: "redis://localhost:6379",
    /** The one dial the task asked to be easy to change: how many concurrent virtual users hit
     * MapService as if they were `heroesPerUser` real users' worth of requests each. */
    val users: Int = 500,
    val heroesPerUser: Int = 15,
    /** Fixed rarity applied to every generated hero (see [StressData.randomHero]); real rarities
     * run 0..5, this defaults to 5 (top tier) since that's the stake/reward path worth stressing. */
    val heroRarity: Int = 5,
    val bombsPerHero: Int = 6,
    val durationSeconds: Int = 60,
    val rampUpSeconds: Int = 5,
    val thinkTimeMs: Long = 200,
    /** Fuse length sent on `/auto/start` (matches MapService's own `BOMB_FUSE_MS` default). */
    val fuseMs: Long = 3000,
    val mapDensity: Double = 0.35,
    val mapResetPauseMs: Long = 3000,
    // Roster edits (a hero leaving or rejoining) per user per minute.
    val churnPerMinute: Double = 2.0,
    val blockPassRatio: Double = 0.1,
    val airdropRatio: Double = 0.2,
    val printIntervalSeconds: Int = 5,
    val seed: Long = System.currentTimeMillis(),
    /** Directory (relative to the working directory, created if missing) that the run's full
     * console output -- startup line, every periodic report, and the final report -- is saved
     * into as a timestamped .log file, alongside printing it live. Pass "" to disable saving. */
    val reportDir: String = "report",
) {
    companion object {
        fun parse(args: Array<String>): StressTestConfig {
            var c = StressTestConfig()
            var i = 0
            fun next(): String {
                i++
                if (i >= args.size) {
                    System.err.println("Missing value for ${args[i - 1]}")
                    exitProcess(1)
                }
                return args[i]
            }
            while (i < args.size) {
                when (args[i]) {
                    "--base-url" -> c = c.copy(baseUrl = next())
                    "--redis-url" -> c = c.copy(redisConnectionString = next())
                    "--users" -> c = c.copy(users = next().toInt())
                    "--heroes-per-user" -> c = c.copy(heroesPerUser = next().toInt())
                    "--hero-rarity" -> c = c.copy(heroRarity = next().toInt())
                    "--bombs-per-hero" -> c = c.copy(bombsPerHero = next().toInt())
                    "--duration" -> c = c.copy(durationSeconds = next().toInt())
                    "--ramp-up" -> c = c.copy(rampUpSeconds = next().toInt())
                    "--think-time-ms" -> c = c.copy(thinkTimeMs = next().toLong())
                    "--fuse-ms" -> c = c.copy(fuseMs = next().toLong())
                    "--map-density" -> c = c.copy(mapDensity = next().toDouble())
                    "--map-reset-pause-ms" -> c = c.copy(mapResetPauseMs = next().toLong())
                    "--churn-per-min" -> c = c.copy(churnPerMinute = next().toDouble())
                    "--block-pass-ratio" -> c = c.copy(blockPassRatio = next().toDouble())
                    "--airdrop-ratio" -> c = c.copy(airdropRatio = next().toDouble())
                    "--print-interval" -> c = c.copy(printIntervalSeconds = next().toInt())
                    "--seed" -> c = c.copy(seed = next().toLong())
                    "--report-dir" -> c = c.copy(reportDir = next())
                    "--help", "-h" -> {
                        printHelp()
                        exitProcess(0)
                    }
                    else -> {
                        System.err.println("Unknown arg: ${args[i]}")
                        printHelp()
                        exitProcess(1)
                    }
                }
                i++
            }
            return c
        }

        private fun printHelp() {
            println(
                """
                MapService stress test client -- simulates N concurrent users calling MapService
                directly (the same way bombcrypto-server-v2's UserBlockMapManagerV2 would), without
                needing a real server or client in the loop.

                Usage: ./gradlew stressTest -Pargs="[options]"
                  --base-url <url>            MapService base URL (default $DEFAULT_BASE_URL
                                               or ${'$'}MAP_SERVICE_URL)
                  --redis-url <url>            Redis MapService publishes treasure events to, same
                                               one the events are read back from (default
                                               redis://localhost:6379 or ${'$'}REDIS_CONNECTION_STRING)
                  --users <n>                  Number of concurrent virtual users (default 10)
                  --heroes-per-user <n>        Heroes simulated per user (default 15)
                  --hero-rarity <n>            Fixed rarity for every generated hero (default 5)
                  --bombs-per-hero <n>         Max concurrent planted bombs per hero (default 6)
                  --duration <seconds>         Test duration (default 60)
                  --ramp-up <seconds>          Spread user start times over this window (default 5)
                  --think-time-ms <ms>         Delay between ticks per user (default 200)
                  --fuse-ms <ms>               Bomb fuse length sent on auto/start (default 3000)
                  --map-density <0..1>         Block density for generated maps (default 0.35)
                  --map-reset-pause-ms <ms>    Heroes' pause after a new map (default 3000)
                  --churn-per-min <n>          Roster edits per user per minute (default 2)
                  --block-pass-ratio <0..1>    Fraction of block-pass heroes (default 0.1)
                  --airdrop-ratio <0..1>       Fraction of heroes flagged as airdrop users (default 0.2)
                  --print-interval <seconds>   Periodic report interval (default 5)
                  --seed <long>                RNG seed, for reproducible runs (default: current time)
                  --report-dir <dir>           Save full console output here as a timestamped .log
                                               file (default "report"; pass "" to disable)
                """.trimIndent()
            )
        }
    }
}
