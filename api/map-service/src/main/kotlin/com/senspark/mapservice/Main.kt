package com.senspark.mapservice

import com.senspark.mapservice.auto.AutoPlayManager
import com.senspark.mapservice.domain.SessionStore
import com.senspark.mapservice.model.ErrorResponse
import com.senspark.mapservice.redis.RedisConnector
import com.senspark.mapservice.redis.RedisPubSubTreasureEventPublisher
import com.senspark.mapservice.redis.TreasureEventPublisher
import com.senspark.mapservice.routes.autoRoutes
import com.senspark.mapservice.routes.sessionRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import java.time.Duration

/** Matches the main server's `time_bomb_explode` default. */
const val DEFAULT_FUSE_MS = 3000L

fun main() {
    val port = System.getenv("MAP_SERVICE_PORT")?.toIntOrNull() ?: 8090
    embeddedServer(Netty, port = port, module = Application::mapServiceModule).start(wait = true)
}

/** Production module: treasure events go to the same Redis the main game server uses. */
fun Application.mapServiceModule() {
    // Same env var name (and format) as bombcrypto-server-v2's REDIS_CONNECTION_STRING.
    val redisConnectionString = System.getenv("REDIS_CONNECTION_STRING") ?: "redis://localhost:6379"
    val defaultFuseMs = System.getenv("BOMB_FUSE_MS")?.toLongOrNull() ?: DEFAULT_FUSE_MS
    val redisRetryMs = System.getenv("REDIS_RETRY_MS")?.toLongOrNull() ?: 3000
    // Connects in the background; MapService serves requests even while Redis is down.
    val redis = RedisConnector(redisConnectionString, Duration.ofMillis(redisRetryMs))
    val treasurePublisher = RedisPubSubTreasureEventPublisher(redis)
    environment.monitor.subscribe(ApplicationStopped) { redis.close() }
    val autoLeaseMs = System.getenv("AUTO_LEASE_MS")?.toLongOrNull() ?: AutoPlayManager.DEFAULT_LEASE_MS
    mapServiceModule(treasurePublisher, defaultFuseMs, autoLeaseMs = autoLeaseMs)
}

fun Application.mapServiceModule(
    treasurePublisher: TreasureEventPublisher,
    defaultFuseMs: Long = DEFAULT_FUSE_MS,
    autoClock: () -> Long = System::currentTimeMillis,
    autoLeaseMs: Long = AutoPlayManager.DEFAULT_LEASE_MS,
) {
    val sessionStore = SessionStore()

    val autoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    environment.monitor.subscribe(ApplicationStopped) { autoScope.cancel() }
    val autoPlay = AutoPlayManager(sessionStore, treasurePublisher, autoScope, autoClock, leaseMs = autoLeaseMs)
    sessionStore.startIdleSweep(this) { key -> autoPlay.cancel(key) }

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
    install(CallLogging) {
        level = Level.INFO
        // Only failed calls: keepalive/roster traffic would otherwise log on every request.
        filter { call -> (call.response.status()?.value ?: 0) >= 400 }
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(cause.message ?: "internal_error"),
            )
        }
    }

    routing {
        get("/health") { call.respondText("ok") }
        sessionRoutes(sessionStore, autoPlay)
        autoRoutes(sessionStore, autoPlay, defaultFuseMs)
    }
}
