package com.senspark.game.manager.blockMap

import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.user.BlockMap
import com.senspark.game.declare.EnumConstants
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject

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
}