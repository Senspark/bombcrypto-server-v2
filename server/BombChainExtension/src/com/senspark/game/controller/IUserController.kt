package com.senspark.game.controller

import com.senspark.common.service.IServiceLocator
import com.senspark.common.utils.IServerLogger
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.data.model.user.PvPRank
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.KickReason
import com.senspark.game.extension.ServerServices
import com.senspark.game.manager.IMasterUserManager
import com.senspark.game.user.IUserPermissions
import com.senspark.game.user.SkinChest
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import java.sql.Timestamp

interface IUserController {
    val fullName: String?
    val serviceLocator: IServiceLocator
    val userId: Int
    val user: User?
    val userInfo: IUserInfo
    val userName: String
    val walletAddress: String
    val dataType: EnumConstants.DataType
    var masterUserManager: IMasterUserManager
    val locker: Any
    val svServices: ServerServices
    val logger: IServerLogger
    val pvpRank: PvPRank

    fun isAirdropUser(): Boolean

    fun setNeedSave(key: EnumConstants.SAVE)
    fun saveGameAndLoadReward()
    fun initDependencies(): Boolean
    fun setUserInfo(userInfo: IUserInfo)
    fun setUser(user: User)
    fun verifyAndUpdateUserHash(): Boolean

    fun isInitialized(): Boolean
    fun checkHash(): Boolean

    fun send(cmdName: String, data: ISFSObject, log: Boolean = true)
    fun sendDataEncryption(cmdName: String, data: ISFSObject, log: Boolean = true)

    fun logOut()
    fun disconnect(reason: KickReason)

    fun dispose()
    
    fun joinPvpQueue(
        mode: Int,
        matchId: String,
        test: Boolean,
        heroId: Int,
        boosters: List<Int>,
        pings: Map<String, Int>,
        avatar: Int,
        gameMode: Int,
        wagerMode: Int,
        wagerTier: Int,
        wagerToken: Int
    )

    fun countUserRanked(): Int
    val pvPRanking: ISFSObject
    val pvPRankingList: ISFSArray
    fun updatePvpRanking(point: Int, match: Int, win: Int)
    fun loadReward()
    fun ban(isBan: Int, banReason: String, banExpired: Timestamp?)
    val lastPlayedPvPHeroId: Long
    val userPermissions: IUserPermissions
    fun openSkinChest(): SkinChest
    fun leavePvPQueue(): Boolean
    fun getPvPHistory(at: Int, count: Int): ISFSArray
    val pvPConfig: ISFSObject
    fun reloadPvpRanking()
}