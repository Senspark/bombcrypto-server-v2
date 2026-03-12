package com.senspark.game.api

import com.senspark.common.cache.IMessengerService
import com.senspark.common.constant.PVPInternalCommand
import com.senspark.game.api.redis.IMatch
import com.senspark.game.api.redis.IUser
import com.senspark.game.api.redis.RedisPvpFoundMatchApi
import com.senspark.game.data.manager.IMasterDataManager
import com.senspark.game.declare.SFSCommand
import com.senspark.game.manager.IUsersManager
import com.senspark.game.manager.pvp.GlobalMatchmaker
import com.senspark.game.pvp.IPvpResultManager
import com.senspark.game.pvp.manager.IPvpQueueManager
import com.senspark.lib.data.manager.GameConfigManager
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GameInternalMessageHandler(
    private val _queueManager: IPvpQueueManager,
    private val _masterDataManager: IMasterDataManager,
    private val _pvpResultManager: IPvpResultManager,
    private val _messengerService: IMessengerService,
    private val _usersManager: IUsersManager,
    private val _gameConfigManager: IGameConfigManager,
) : IInternalMessageHandler {

    private val _matchmaker: GlobalMatchmaker = _queueManager.matchMaker
    private val _redisPvpFoundMatchApi = RedisPvpFoundMatchApi(_messengerService)

    override fun handle(command: String, params: ISFSObject): ISFSObject? {
        return when (command) {
            SFSCommand.GET_GAME_CONFIG -> handleGetGameConfig(params)
            else -> null
        }
    }

    override fun handle(command: String, params: String): ISFSObject? {
        when (command) {
            PVPInternalCommand.PVP_FOUND_MATCH -> handleFoundMatch(params)
            PVPInternalCommand.PVP_END_MATCH -> handleEndMatch(params)
        }
        return null
    }

    private fun handleFoundMatch(data: String) {
        val info = _redisPvpFoundMatchApi.parse(data)
        //Kiểm tra user có tồn tại ko, trong trường hợp có nhiều server
        if (isValidUser(info)) {
            val userList = mutableListOf<IUser>()
            for ((index, user) in info.users.withIndex()) {
                userList.add(user)
            }


            val id = info.id
            val serverId = info.zone
            val serverDetail = info.serverDetail
            val rule = info.rule
            val team = info.team
            _matchmaker.onMatchFound(id, serverId, serverDetail, rule, team, userList)
        }
        /*
        val info = PvpFoundMatchInfo.parse(data)
        _matchmaker.onMatchFound(
            info.id,
            info.serverId,
            info.timestamp,
            info.mode,
            info.rule,
            info.team,
            info.userId,
            info.slot,
            info.data,
        )
        return SFSObject()
        */
    }

    private fun isValidUser(match: IMatch): Boolean {
        for (user in match.users) {
            val id = _usersManager.getUserId(user.id)
            if (id == -1 && !user.data.isBot) {
                return false
            }
        }
        return true
    }

    private fun isValidUser(userName: String): Boolean {
        val id = _usersManager.getUserId(userName)
        if (id == -1) {
            return false
        }
        return true
    }

    private fun handleEndMatch(data: String): ISFSObject {
        val resultInfo = PvpResultInfo.parse(data)
        //Kiểm tra user có tồn tại ko, trong trường hợp có nhiều server
        if (isValidUser(resultInfo)) {
            _pvpResultManager.handleResult(resultInfo)
        }
        return SFSObject()
    }


    private fun isValidUser(result: IPvpResultInfo): Boolean {
        for (userInfo in result.info) {
            val id = _usersManager.getUserId(userInfo.username)
            if (id == -1 && !userInfo.isBot) {
                return false
            }
        }
        return true
    }


    private fun handleGetGameConfig(data: ISFSObject): ISFSObject {
        val clientVersion = data.getInt("client_version")
        if (!checkVersion(clientVersion)) {
            throw Exception("Wrong version")
        }
        return _masterDataManager.getGameConfig(clientVersion)
    }

    private fun checkVersion(versionCode: Int): Boolean {
        return versionCode >= _gameConfigManager.minVersionCanPlay
    }
}