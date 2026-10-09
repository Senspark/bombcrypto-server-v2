package com.senspark.mapservice.routes

import com.senspark.mapservice.auto.AutoPlayManager
import com.senspark.mapservice.domain.GlobalConfigStore
import com.senspark.mapservice.domain.MapGrid
import com.senspark.mapservice.domain.RewardConfig
import com.senspark.mapservice.domain.SessionConfig
import com.senspark.mapservice.domain.SessionStore
import com.senspark.mapservice.logging.TraceLog
import com.senspark.mapservice.model.ConfigUpdateRequest
import com.senspark.mapservice.model.ErrorResponse
import com.senspark.mapservice.model.MapInitRequest
import com.senspark.mapservice.model.MapReplaceRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post

fun Route.sessionRoutes(store: SessionStore, auto: AutoPlayManager) {
    post("/config") {
        val body = call.receive<ConfigUpdateRequest>()
        body.rewardConfig?.let { GlobalConfigStore.updateReward(RewardConfig.from(it)) }
        call.respond(HttpStatusCode.OK)
    }

    post("/sessions/{key}/init") {
        val key = call.parameters["key"]!!
        val body = call.receive<MapInitRequest>()
        val map = MapGrid.fromDtos(body.blocks, body.tileset, body.mode)
        val config = SessionConfig(
            reward = body.rewardConfig?.let { RewardConfig.from(it) } ?: GlobalConfigStore.current.reward,
        )
        val created = store.create(key, map, config)
        TraceLog.info { "[INIT] session=$key created=$created blockCount=${body.blocks.size}" }
        if (!created) {
            call.respond(HttpStatusCode.Conflict, ErrorResponse("session_already_exists"))
            return@post
        }
        call.respond(HttpStatusCode.OK)
    }

    post("/sessions/{key}/map") {
        val key = call.parameters["key"]!!
        val body = call.receive<MapReplaceRequest>()
        val map = MapGrid.fromDtos(body.blocks, body.tileset, body.mode)
        val replaced = store.replaceMap(key, map)
        if (!replaced) {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("session_not_found"))
            return@post
        }
        // Auto mode already queued NEW_MAP (respawned heroes); let its runner publish and resume.
        auto.signal(key)
        call.respond(HttpStatusCode.OK)
    }

    delete("/sessions/{key}") {
        val key = call.parameters["key"]!!
        store.delete(key)
        auto.cancel(key)
        call.respond(HttpStatusCode.OK)
    }
}
