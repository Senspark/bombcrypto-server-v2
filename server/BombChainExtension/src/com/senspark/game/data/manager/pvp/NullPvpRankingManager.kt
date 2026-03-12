package com.senspark.game.data.manager.pvp

import com.senspark.game.data.model.user.PvPRank
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray

class NullPvpRankingManager : IPvpRankingManager {

    override fun initialize() {
    }

    override fun reload() {}

    override fun getRanking(userId: Int): PvPRank? {
        return null
    }

    override fun getRanking(name: String, userId: Int, dataType: DataType): PvPRank {
        throw CustomException("Feature not support")
    }

    override fun getTotalCount(): Int {
        return 0
    }

    override fun toSFSArray(): ISFSArray {
        return SFSArray()
    }

    override fun decayUserRank() {}

    override fun getDecayPointUser(uid: Int): Int {
        return 0
    }

    override fun clearDataForOneUser(uid: Int) {
    }
}