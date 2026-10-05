package com.senspark.game.manager.blockMap

import com.senspark.game.declare.EnumConstants.SAVE
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.blockMap.mapservice.IMapTreasureEventListener
import com.smartfoxserver.v2.entities.data.ISFSObject

// Treasure-mode map manager backed by MapService (see server/docs/treasure_server_driven.md).
// MapService failures surface as ErrorCode.MAP_SERVICE_ERROR.
interface IUserBlockMapManagerV2 : IMapTreasureEventListener {
    val locker: Any

    fun saveMap(userId: Int, needSave: MutableMap<SAVE, Boolean>)

    @Throws(CustomException::class)
    fun getBlockMap(): ISFSObject

    // Call once on logout.
    fun notifySessionEnd()

    // Server-driven treasure mode (server/docs/treasure_server_driven.md): MapService plays every
    // working hero and the result is pushed as TREASURE_EVENTS. Returns the START_TREASURE_MODE payload.
    // [paused]: the client is paused, so a resync never makes heroes walk behind a paused screen.
    @Throws(CustomException::class)
    fun startTreasureMode(paused: Boolean = false): ISFSObject

    fun stopTreasureMode()

    // Client paused/resumed: heroes halt until resumed, bombs already planted still explode.
    fun setTreasurePaused(paused: Boolean)

    // Call after any hero stage/active change; adds/removes heroes from the running game. No-op when stopped.
    fun syncTreasureHeroes()
}
