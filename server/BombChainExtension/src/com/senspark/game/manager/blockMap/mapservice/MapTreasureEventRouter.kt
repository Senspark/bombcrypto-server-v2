package com.senspark.game.manager.blockMap.mapservice

import com.senspark.common.service.IGlobalService
import com.senspark.common.utils.ILogger
import com.senspark.game.utils.deserialize
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

interface IMapTreasureEventListener {
    fun onTreasureEvents(batch: MsTreasureEventBatch)
}

// Routes AP_MAP_TREASURE_EVENT_CHANNEL batches to the manager owning the session key.
interface IMapTreasureEventRouter : IGlobalService {
    // Latest registration wins (re-login replaces the old manager).
    fun register(sessionKey: String, listener: IMapTreasureEventListener)

    fun unregister(sessionKey: String, listener: IMapTreasureEventListener)

    // True if this instance owns the session; false means another server owns it.
    fun handle(rawMessage: String): Boolean
}

// One session's batches run in arrival order on one stripe; different users run in parallel.
class MapTreasureEventRouter(
    private val _logger: ILogger,
    private val _executorFor: (String) -> Executor = stripedExecutors(4),
) : IMapTreasureEventRouter {
    companion object {
        fun stripedExecutors(stripes: Int): (String) -> Executor {
            val executors: List<ExecutorService> = List(stripes) { index ->
                Executors.newSingleThreadExecutor { runnable ->
                    Thread(runnable, "map-treasure-event-$index").apply { isDaemon = true }
                }
            }
            return { key -> executors[Math.floorMod(key.hashCode(), stripes)] }
        }
    }

    private val _listeners = ConcurrentHashMap<String, IMapTreasureEventListener>()

    override fun initialize() {
    }

    override fun register(sessionKey: String, listener: IMapTreasureEventListener) {
        _listeners[sessionKey] = listener
    }

    override fun unregister(sessionKey: String, listener: IMapTreasureEventListener) {
        _listeners.remove(sessionKey, listener)
    }

    override fun handle(rawMessage: String): Boolean {
        val batch = try {
            deserialize<MsTreasureEventBatch>(rawMessage)
        } catch (e: Exception) {
            _logger.error("[MAP_TREASURE_EVENT] unparseable message: $rawMessage", e)
            return true
        }
        val listener = _listeners[batch.sessionKey] ?: return false
        _executorFor(batch.sessionKey).execute {
            try {
                listener.onTreasureEvents(batch)
            } catch (e: Exception) {
                _logger.error("[MAP_TREASURE_EVENT] handling failed session=${batch.sessionKey} seq=${batch.events.firstOrNull()?.seq}", e)
            }
        }
        return true
    }
}
