package com.senspark.game.manager.blockMap.mapservice

class MapServiceException(message: String, cause: Throwable? = null) : Exception(message, cause)

// 404: MapService lost the session (e.g. restart); caller should re-init and retry once.
class MapServiceSessionNotFoundException : Exception("MapService session not found")

interface IMapServiceClient {
    // 409 (already exists) is treated as success.
    fun initSession(sessionKey: String, request: MsMapInitRequest)

    fun replaceMap(sessionKey: String, request: MsMapReplaceRequest)

    // Idempotent.
    fun deleteSession(sessionKey: String)

    // Server-driven treasure mode. A restart while running is a resync (keeps positions and bombs).
    fun autoStart(sessionKey: String, request: MsAutoStartRequest): MsAutoSnapshotDto

    // False if auto mode isn't running (409).
    fun autoHeroes(sessionKey: String, request: MsAutoHeroesRequest): Boolean

    // Heroes halt until resumed; live bombs still explode. False if auto mode isn't running (409).
    fun autoPause(sessionKey: String, paused: Boolean): Boolean

    fun autoStop(sessionKey: String)

    fun autoKeepalive(sessionKey: String): MsAutoKeepaliveResponse
}
