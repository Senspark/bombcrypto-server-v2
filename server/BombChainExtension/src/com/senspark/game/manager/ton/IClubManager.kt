package com.senspark.game.manager.ton

import com.senspark.common.service.IServerService
import com.senspark.game.controller.IUserController
import com.smartfoxserver.v2.entities.data.ISFSObject

interface IClubManager : IServerService {
    fun getClubInfo(uid: Int): ISFSObject
    fun getClubInfoById(clubId: Int): ISFSObject
    fun getListClub(): ISFSObject
    fun getTopBidClub(): ISFSObject
    fun getBidPackage(clubId: Int): MutableMap<Int, Int>
    fun createClub(json: String)
    fun createClub(telegramId: Long, name: String, link: String, avatarName: String): Int
    fun createClubV3(uid: Int, nameClub: String)
    fun joinClubV3(uid: Int, nameClub: String)
    fun joinClub(json: String)
    fun joinClub(uid: Int, displayName: String, clubId: Int, isForceLeave: Boolean)
    fun leaveClub(json: String)
    fun leaveClub(uid: Int)
    fun addMemberPoint(uid: Int, point: Double, season: Int)
    fun summaryClubPoint()
    fun boostClub(userController: IUserController, clubId: Int, packageId: Int)
    fun summaryClubBid()
    fun getClubIdByTelegramId(telegramId: Long): Int?
}

