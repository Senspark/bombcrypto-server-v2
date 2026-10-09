package com.senspark.mapservice.stresstest

import com.senspark.mapservice.model.AutoHeroesRequest
import com.senspark.mapservice.model.AutoKeepaliveResponse
import com.senspark.mapservice.model.AutoSnapshotDto
import com.senspark.mapservice.model.AutoStartRequest
import com.senspark.mapservice.model.ConfigUpdateRequest
import com.senspark.mapservice.model.MapInitRequest
import com.senspark.mapservice.model.MapReplaceRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** How many distinct exceptions to print per endpoint before going quiet -- enough to diagnose the
 * failure mode without flooding stdout when thousands of the same error happen under load. */
private const val MAX_LOGGED_ERRORS_PER_ENDPOINT = 20

/**
 * Thin async wrapper around every MapService HTTP route, one method each, each one timed into
 * [metrics]. Calls a *virtual* user makes directly against MapService -- exactly what
 * `bombcrypto-server-v2`'s `MapServiceClient`/`UserBlockMapManagerV2` would do on a real user's
 * behalf, just without a real server or a real logged-in client behind it.
 */
class MapServiceStressClient(
    private val http: HttpClient,
    private val baseUrl: String,
    private val metrics: Metrics,
) {
    private val loggedErrorCounts = ConcurrentHashMap<String, AtomicLong>()

    /** Prints the first [MAX_LOGGED_ERRORS_PER_ENDPOINT] exceptions per endpoint so a run's failure
     * mode (timeout vs. connect-refused vs. something else) is visible instead of only a raw count. */
    private fun logError(name: String, e: Exception) {
        val n = loggedErrorCounts.getOrPut(name) { AtomicLong(0) }.incrementAndGet()
        when {
            n <= MAX_LOGGED_ERRORS_PER_ENDPOINT ->
                System.err.println("[$name] error #$n: ${e::class.simpleName}: ${e.message}")
            n == MAX_LOGGED_ERRORS_PER_ENDPOINT + 1L ->
                System.err.println("[$name] further errors suppressed (already logged $MAX_LOGGED_ERRORS_PER_ENDPOINT)")
        }
    }

    suspend fun pushConfig(request: ConfigUpdateRequest) {
        timed("config") {
            http.post("$baseUrl/config") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    /** Returns true on 200 or 409 (session already existed) -- both are a fine outcome for init. */
    suspend fun initSession(key: String, request: MapInitRequest): Boolean {
        return timed("init") {
            val resp = http.post("$baseUrl/sessions/$key/init") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            resp.status.value == 200 || resp.status.value == 409
        }
    }

    suspend fun replaceMap(key: String, request: MapReplaceRequest) {
        timed("map") {
            http.post("$baseUrl/sessions/$key/map") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun deleteSession(key: String) {
        timed("delete") {
            http.delete("$baseUrl/sessions/$key")
        }
    }

    suspend fun autoStart(key: String, request: AutoStartRequest): AutoSnapshotDto? {
        return timedTagged("auto_start") {
            val resp = http.post("$baseUrl/sessions/$key/auto/start") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (resp.status.isSuccess()) resp.body<AutoSnapshotDto>() to "ok" else null to "http_${resp.status.value}"
        }
    }

    suspend fun autoHeroes(key: String, request: AutoHeroesRequest): Boolean {
        return timedTagged("auto_heroes") {
            val resp = http.post("$baseUrl/sessions/$key/auto/heroes") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            resp.status.isSuccess() to (if (resp.status.isSuccess()) "ok" else "http_${resp.status.value}")
        } ?: false
    }

    suspend fun autoKeepalive(key: String): AutoKeepaliveResponse? {
        return timedTagged("keepalive") {
            val resp = http.post("$baseUrl/sessions/$key/auto/keepalive")
            if (resp.status.isSuccess()) {
                val body = resp.body<AutoKeepaliveResponse>()
                body to (if (body.running) "running" else "stopped")
            } else {
                null to "http_${resp.status.value}"
            }
        }
    }

    suspend fun autoStop(key: String) {
        timed("auto_stop") { http.post("$baseUrl/sessions/$key/auto/stop") }
    }

    /** For calls whose only failure signal callers need is "it threw" -- lets the exception propagate. */
    private suspend fun <T> timed(name: String, block: suspend () -> T): T {
        val start = System.nanoTime()
        try {
            val result = block()
            metrics.endpoint(name).recordSuccess((System.nanoTime() - start) / 1_000_000)
            return result
        } catch (e: Exception) {
            logError(name, e)
            metrics.endpoint(name).recordError()
            throw e
        }
    }

    /** For calls that report their own outcome as a tag (HTTP status or business result); swallows
     * transport exceptions into a null result so one flaky call doesn't kill a virtual user's loop. */
    private suspend fun <T> timedTagged(name: String, block: suspend () -> Pair<T?, String>): T? {
        val start = System.nanoTime()
        return try {
            val (result, tag) = block()
            metrics.endpoint(name).recordSuccess((System.nanoTime() - start) / 1_000_000, tag)
            result
        } catch (e: Exception) {
            logError(name, e)
            metrics.endpoint(name).recordError()
            null
        }
    }
}
