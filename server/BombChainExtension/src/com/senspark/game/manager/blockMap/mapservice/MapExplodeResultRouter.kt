package com.senspark.game.manager.blockMap.mapservice

import com.senspark.common.service.IGlobalService
import com.senspark.common.utils.ILogger
import com.senspark.game.utils.deserialize
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

interface IMapExplodeResultListener {
    fun onExplodeResult(event: MsExplodeResultEvent)
}

// Routes AP_MAP_EXPLODE_RESULT_STR entries to the manager owning the session key.
interface IMapExplodeResultRouter : IGlobalService {
    // Latest registration wins (re-login replaces the old manager).
    fun register(sessionKey: String, listener: IMapExplodeResultListener)

    // Removes only if still mapped to [listener].
    fun unregister(sessionKey: String, listener: IMapExplodeResultListener)

    // True if this instance owns the session (entry gets deleted); false leaves it for other servers.
    fun handle(rawMessage: String): Boolean
}

// Striped single-thread executors: one session's results stay in stream order, users run in parallel.
class MapExplodeResultRouter(
    private val _logger: ILogger,
    private val _executorFor: (String) -> Executor = stripedExecutors(4),
) : IMapExplodeResultRouter {
    companion object {
        fun stripedExecutors(stripes: Int): (String) -> Executor {
            val executors: List<ExecutorService> = List(stripes) { index ->
                Executors.newSingleThreadExecutor { runnable ->
                    Thread(runnable, "map-explode-result-$index").apply { isDaemon = true }
                }
            }
            return { key -> executors[Math.floorMod(key.hashCode(), stripes)] }
        }
    }

    private val _listeners = ConcurrentHashMap<String, IMapExplodeResultListener>()

    override fun initialize() {
    }

    override fun register(sessionKey: String, listener: IMapExplodeResultListener) {
        _listeners[sessionKey] = listener
    }

    override fun unregister(sessionKey: String, listener: IMapExplodeResultListener) {
        _listeners.remove(sessionKey, listener)
    }

    override fun handle(rawMessage: String): Boolean {
        val event = try {
            deserialize<MsExplodeResultEvent>(rawMessage)
        } catch (e: Exception) {
            // Malformed for every instance; delete it.
            _logger.error("[MAP_EXPLODE_RESULT] unparseable entry: $rawMessage", e)
            return true
        }
        val listener = _listeners[event.sessionKey] ?: return false
        _executorFor(event.sessionKey).execute {
            try {
                listener.onExplodeResult(event)
            } catch (e: Exception) {
                _logger.error("[MAP_EXPLODE_RESULT] handling failed session=${event.sessionKey} hero=${event.heroId} bombNo=${event.bombNo}", e)
            }
        }
        return true
    }
}
