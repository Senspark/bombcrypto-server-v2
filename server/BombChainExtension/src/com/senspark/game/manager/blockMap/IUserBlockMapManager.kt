package com.senspark.game.manager.blockMap

import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.user.BlockMap
import com.senspark.game.declare.EnumConstants
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject

enum class PlantBombResult {
    OK,
    TARGET_MISMATCH,
    TOO_FAST,
    NO_BOMB_TO_PLANT,
}

data class TakenBomb(val cell: Pair<Int, Int>, val plantedAt: Long)

interface IUserBlockMapManager {
    val locker: Any
    fun saveMap(userId: Int, needSave: MutableMap<EnumConstants.SAVE, Boolean>)

    @Throws(CustomException::class)
    fun getBlockMap(): ISFSObject

    fun getBombermanDangerous(controller: IUserController): ISFSObject
    fun getBombermanDangerousStatus(hero: Hero): ISFSObject
    fun getBlockExplode(bbm: Hero, colBoom: Int, rowBoom: Int): List<BlockMap>
    fun checkHackExplodeBlock(bbm: Hero, blockArr: ISFSArray): Boolean
    fun checkHackSpeedBombExplode(bbm: Hero, bombNo: Int): Boolean

    fun canSetBoom(col: Int, row: Int): Boolean
    fun explode(bbm: Hero, colBoom: Int, rowBoom: Int, blockArr: ISFSArray): ISFSObject

    // ================== V6 server-assigned bomb targeting ==================
    // See server/docs/explode_v6.md. Callers must hold `locker`.

    fun getOrCreateTarget(heroId: Int, seed: Pair<Int, Int>?): Pair<Int, Int>?

    fun plantBomb(heroId: Int, bombNo: Int, col: Int, row: Int, speed: Int, bombCount: Int): PlantBombResult

    fun takePlantedBomb(heroId: Int, bombNo: Int, col: Int, row: Int): TakenBomb?

    fun rejectTarget(heroId: Int, col: Int, row: Int)

    fun debugTarget(heroId: Int): Pair<Int, Int>?
}