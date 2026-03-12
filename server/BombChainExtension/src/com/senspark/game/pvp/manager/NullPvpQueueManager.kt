package com.senspark.game.pvp.manager

import com.senspark.common.pvp.IMatchInfo
import com.senspark.common.pvp.IMatchUserInfo
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.User
import javax.crypto.SecretKey

class NullPvpQueueManager : IPvpQueueManager {

    override val matchMaker get() = throw CustomException("Feature not support")

    override fun initialize() {
    }

    override fun join(
        user: User,
        username: String,
        pings: Map<String, Int>,
        info: IMatchUserInfo,
        aesKey: SecretKey
    ): Boolean {
        return false
    }

    override fun keepJoining(username: String) {}

    override fun leave(username: String): Boolean {
        return false
    }

    override fun destroy() {}

    override fun onMatchFound(username: String, info: IMatchInfo) {}

    override fun find(username: String): Pair<User, SecretKey>? {
        return null
    }
}