package com.senspark.game.pvp.manager

import com.senspark.common.pvp.IMatchInfo
import com.senspark.common.pvp.IMatchUserInfo
import com.senspark.common.service.IServerService
import com.senspark.common.service.IService
import com.senspark.game.manager.pvp.GlobalMatchmaker
import com.senspark.game.manager.pvp.IMatchmakerListener
import com.senspark.game.utils.IUserFinder
import com.smartfoxserver.v2.entities.User
import javax.crypto.SecretKey

interface IPvpQueueManager : IService, IUserFinder, IMatchmakerListener, IServerService {
    val matchMaker: GlobalMatchmaker

    fun join(
        user: User,
        username: String,
        pings: Map<String, Int>,
        info: IMatchUserInfo,
        aesKey: SecretKey
    ): Boolean

    fun keepJoining(username: String)

    fun leave(username: String): Boolean

    override fun destroy() {}

    override fun onMatchFound(username: String, info: IMatchInfo)

    override fun find(username: String): Pair<User, SecretKey>?
}