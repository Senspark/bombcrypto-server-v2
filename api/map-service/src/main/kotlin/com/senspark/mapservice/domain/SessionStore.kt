package com.senspark.mapservice.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-wide registry of [Session]s, keyed by `"$userId-$dataType-$mode"`. Since MapService is a
 * single long-running process serving every user (unlike the original `UserBlockMapManagerImpl`,
 * which was one JVM object per logged-in user), this is the thing that gives each user session its
 * own isolated map/target/plant state.
 *
 * Idle sessions are swept after [idleTimeoutMs] as a safety net for a missed `DELETE` on logout
 * (crash, force-kill) -- bounds memory even if the main server's teardown call never arrives.
 */
class SessionStore(private val idleTimeoutMs: Long = 45 * 60 * 1000L) {
    private val sessions = ConcurrentHashMap<String, Session>()

    fun get(key: String): Session? = sessions[key]

    fun create(key: String, map: MapGrid, config: SessionConfig): Boolean {
        val session = Session(map, config)
        return sessions.putIfAbsent(key, session) == null
    }

    fun replaceMap(key: String, map: MapGrid): Boolean {
        val session = sessions[key] ?: return false
        session.touch()
        session.replaceMap(map)
        return true
    }

    fun delete(key: String) {
        sessions.remove(key)
    }

    fun startIdleSweep(scope: CoroutineScope, intervalMs: Long = 5 * 60 * 1000L, onEvict: (String) -> Unit = {}) {
        scope.launch(Dispatchers.Default) {
            while (true) {
                delay(intervalMs)
                val now = System.currentTimeMillis()
                val expired = sessions.entries.filter { (_, session) -> now - session.lastActivityMs > idleTimeoutMs }
                for ((key, session) in expired) {
                    if (sessions.remove(key, session)) onEvict(key)
                }
            }
        }
    }
}
