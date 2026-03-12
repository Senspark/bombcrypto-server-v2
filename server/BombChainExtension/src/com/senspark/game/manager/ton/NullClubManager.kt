package com.senspark.game.manager.ton

import com.senspark.game.controller.IUserController
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class NullClubManager : IClubManager {
    override fun initialize() {
    }
    
    override fun getClubInfo(uid: Int): ISFSObject {
        return SFSObject()
    }

    override fun getClubInfoById(clubId: Int): ISFSObject {
        return SFSObject()
    }

    override fun getListClub(): ISFSObject {
        return SFSObject()
    }

    override fun getTopBidClub(): ISFSObject {
        return SFSObject()
    }

    override fun getBidPackage(clubId: Int): MutableMap<Int, Int> {
        return mutableMapOf()
    }

    override fun createClub(json: String) {}

    override fun createClub(telegramId: Long, name: String, link: String, avatarName: String): Int {
        return 0
    }
    override fun createClubV3(uid: Int, nameClub: String) {}

    override fun joinClubV3(uid: Int, nameClub: String) {}

    override fun joinClub(json: String) {}

    override fun joinClub(uid: Int, displayName: String, clubId: Int, isForceLeave: Boolean) {}

    override fun leaveClub(json: String) {}

    override fun leaveClub(uid: Int) {}

    override fun addMemberPoint(uid: Int, point: Double, season: Int) {}

    override fun summaryClubPoint() {}

    override fun boostClub(userController: IUserController, clubId: Int, packageId: Int) {}

    override fun summaryClubBid() {}
    override fun getClubIdByTelegramId(idTelegram: Long): Int? {
        return null
    }
}