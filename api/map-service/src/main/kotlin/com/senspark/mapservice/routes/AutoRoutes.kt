package com.senspark.mapservice.routes

import com.senspark.mapservice.auto.AutoConfig
import com.senspark.mapservice.auto.AutoPlay
import com.senspark.mapservice.auto.AutoPlayManager
import com.senspark.mapservice.domain.Session
import com.senspark.mapservice.domain.SessionStore
import com.senspark.mapservice.logging.TraceLog
import com.senspark.mapservice.model.AutoHeroesRequest
import com.senspark.mapservice.model.AutoKeepaliveResponse
import com.senspark.mapservice.model.AutoPauseRequest
import com.senspark.mapservice.model.AutoSeqResponse
import com.senspark.mapservice.model.AutoStartRequest
import com.senspark.mapservice.model.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

// Server-driven treasure mode: MapService plays every hero and publishes events to AP_MAP_TREASURE_EVENT_CHANNEL.
fun Route.autoRoutes(store: SessionStore, auto: AutoPlayManager, defaultFuseMs: Long) {
    suspend fun ApplicationCall.session(): Pair<String, Session>? {
        val key = parameters["key"]!!
        val session = store.get(key)
        if (session == null) {
            respond(HttpStatusCode.NotFound, ErrorResponse("session_not_found"))
            return null
        }
        session.touch()
        return key to session
    }

    post("/sessions/{key}/auto/start") {
        val (key, session) = call.session() ?: return@post
        val body = call.receive<AutoStartRequest>()
        if (body.heroes.any { it.speed < 0 || it.bombCount < 0 }) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_hero"))
            return@post
        }
        val config = AutoConfig(
            fuseMs = (body.fuseMs ?: defaultFuseMs).coerceAtLeast(0),
            mapResetPauseMs = (body.mapResetPauseMs ?: AutoPlay.DEFAULT_MAP_RESET_PAUSE_MS).coerceAtLeast(0),
        )
        val snapshot = auto.start(key, session, body.heroes, config, body.paused)
        TraceLog.info { "[AUTO_START] session=$key heroes=${body.heroes.map { it.hero.heroId }} seq=${snapshot.seq} paused=${body.paused}" }
        call.respond(snapshot)
    }

    post("/sessions/{key}/auto/heroes") {
        val (key, session) = call.session() ?: return@post
        val body = call.receive<AutoHeroesRequest>()
        var seq = 0L
        val applied = auto.update(key, session) { now ->
            if (body.remove.isNotEmpty()) removeHeroes(body.remove, body.reason, now)
            if (body.upsert.isNotEmpty()) upsertHeroes(body.upsert, now)
            seq = this.seq
        }
        if (!applied) {
            call.respond(HttpStatusCode.Conflict, ErrorResponse("auto_mode_not_running"))
            return@post
        }
        call.respond(AutoSeqResponse(seq))
    }

    // Player paused/resumed treasure mode: heroes halt until resumed, live bombs still explode.
    post("/sessions/{key}/auto/pause") {
        val (key, session) = call.session() ?: return@post
        val body = call.receive<AutoPauseRequest>()
        var seq = 0L
        val applied = auto.update(key, session) { now ->
            setPaused(body.paused, now)
            seq = this.seq
        }
        if (!applied) {
            call.respond(HttpStatusCode.Conflict, ErrorResponse("auto_mode_not_running"))
            return@post
        }
        TraceLog.info { "[AUTO_PAUSE] session=$key paused=${body.paused} seq=$seq" }
        call.respond(AutoSeqResponse(seq))
    }

    post("/sessions/{key}/auto/stop") {
        val (key, session) = call.session() ?: return@post
        auto.update(key, session) { now -> stop(now) }
        call.respond(HttpStatusCode.OK)
    }

    // The game server's periodic lease: keeps the session from the idle sweep and its game running.
    post("/sessions/{key}/auto/keepalive") {
        val (_, session) = call.session() ?: return@post
        auto.keepalive(session)
        val autoPlay = session.autoPlay
        call.respond(AutoKeepaliveResponse(running = session.isAutoRunning, seq = autoPlay?.seq ?: 0, paused = autoPlay?.paused == true))
    }

    get("/sessions/{key}/auto") {
        val (_, session) = call.session() ?: return@get
        val snapshot = auto.snapshot(session)
        if (snapshot == null) {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("auto_mode_not_started"))
            return@get
        }
        call.respond(snapshot)
    }
}
