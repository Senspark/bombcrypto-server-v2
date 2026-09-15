package com.senspark.game.manager.blockMap

import com.senspark.game.declare.EnumConstants.SAVE
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.blockMap.mapservice.IMapExplodeResultListener
import com.smartfoxserver.v2.entities.data.ISFSObject

data class HeroTargetRequest(
    val heroId: Int,
    val seed: Pair<Int, Int>?,
    val reject: Pair<Int, Int>?,
)

// Heroes with no legal target are absent from the result.
data class HeroTargetResult(val heroId: Int, val target: Pair<Int, Int>)

data class PlantBombV2Outcome(
    val result: PlantBombResult,
    val nextTarget: Pair<Int, Int>?,
    val hackFlag: Boolean = false,
)

// Treasure-mode map manager backed by MapService (see server/docs/explode_v6.md).
// MapService failures surface as ErrorCode.MAP_SERVICE_ERROR.
interface IUserBlockMapManagerV2 : IMapExplodeResultListener {
    val locker: Any

    fun saveMap(userId: Int, needSave: MutableMap<SAVE, Boolean>)

    @Throws(CustomException::class)
    fun getBlockMap(): ISFSObject

    @Throws(CustomException::class)
    fun getOrCreateTargets(requests: List<HeroTargetRequest>): List<HeroTargetResult>

    // On OK MapService arms the fuse; the result arrives async in [onExplodeResult].
    @Throws(CustomException::class)
    fun plantBomb(heroId: Int, bombNo: Int, col: Int, row: Int, speed: Int, bombCount: Int): PlantBombV2Outcome

    // Call once on logout.
    fun notifySessionEnd()
}
