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

    fun getTargets(sessionKey: String, request: MsTargetsRequest): MsTargetsResponse

    fun plantBomb(sessionKey: String, request: MsPlantRequest): MsPlantResponse

    fun explode(sessionKey: String, request: MsExplodeRequest): MsExplodeResponse
}
