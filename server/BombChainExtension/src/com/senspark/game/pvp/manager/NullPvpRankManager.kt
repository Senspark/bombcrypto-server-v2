package com.senspark.game.pvp.manager

import com.senspark.common.data.IBombRank
import com.senspark.common.pvp.IRankManager
import com.senspark.common.pvp.IRankResult
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray

class NullPvpRankManager : IRankManager {

    override fun initialize() {
    }

    override fun getRank(point: Int): Int {
        return 0
    }

    override fun getBombRank(point: Int): IBombRank {
        throw CustomException("Feature not support")
    }

    override fun calculate(
        isDraw: Boolean,
        isWinner: Boolean,
        slot: Int,
        boosters: List<Int>,
        points: List<Int>
    ): IRankResult {
        throw CustomException("Feature not support")
    }

    override fun toSFSArray(): ISFSArray {
        return SFSArray()
    }

    override fun getAmountPvpMatchesCurrentDate(uid: Int): Int {
        return 0
    }

    override fun destroy() = Unit
}