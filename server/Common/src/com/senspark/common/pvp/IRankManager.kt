package com.senspark.common.pvp

import com.senspark.common.data.IBombRank
import com.senspark.common.service.IServerService
import com.senspark.common.service.IService
import com.smartfoxserver.v2.entities.data.ISFSArray

interface IRankResult {
    /** Final rank. */
    val rank: Int

    /** Final point. */
    val point: Int

    /** Delta point (positive or negative). */
    val deltaPoint: Int

    /** Used boosters. */
    val usedBoosters: List<Int>
}

interface IRankManager : IService, IServerService {
    /** Gets rank for the specified point, one-indexed. */
    fun getRank(point: Int): Int
    fun getBombRank(point: Int): IBombRank
    fun calculate(
        isDraw: Boolean,
        isWinner: Boolean,
        slot: Int,
        boosters: List<Int>,
        points: List<Int>,
    ): IRankResult

    /** Legacy. */
    fun toSFSArray(): ISFSArray
    fun getAmountPvpMatchesCurrentDate(uid: Int): Int
}