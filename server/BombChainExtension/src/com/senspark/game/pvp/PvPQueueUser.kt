package com.senspark.game.pvp

import com.senspark.game.controller.LegacyUserController
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.time.Instant

class PvPQueueUser(
    val controller: LegacyUserController,
    override val isWhitelisted: Boolean,
    override val point: IUserPoint,
    override val bet: Int,
    override var isInMatch: Boolean,
    val joiningTime: Instant,
) : IQueueUser {
    override fun isMatch(other: IQueueUser, delta: Pair<Int, Int>): Boolean {
        return other.bet == bet
            && (point.value - other.point.value) in delta.first..delta.second
            && other.isWhitelisted == isWhitelisted && !isInMatch && !other.isInMatch
    }

    fun toSFSObject(): ISFSObject {
        val result = SFSObject()
        result.putLong("joining_time", joiningTime.toEpochMilli())
        result.putInt("point", point.value)
        result.putUtfString("wallet_address", controller.walletAddress)
        return result
    }
}