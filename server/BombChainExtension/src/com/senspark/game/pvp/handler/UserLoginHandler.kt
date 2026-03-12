package com.senspark.game.pvp.handler

import com.senspark.common.pvp.*
import com.senspark.common.utils.ILogger
import com.senspark.game.extension.PvpZoneExtension
import com.senspark.game.manager.IPvpEnvManager
import com.senspark.game.manager.pvp.InvalidMatchHashException
import com.senspark.game.manager.pvp.MatchExpiredException
import com.senspark.game.pvp.info.*
import com.senspark.game.pvp.manager.IMatchManager
import com.senspark.game.pvp.utility.JsonUtility
import com.senspark.game.utils.ServerError
import com.smartfoxserver.bitswarm.sessions.ISession
import com.smartfoxserver.v2.core.ISFSEvent
import com.smartfoxserver.v2.core.SFSEventParam
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.exceptions.SFSErrorData
import com.smartfoxserver.v2.exceptions.SFSLoginException
import com.smartfoxserver.v2.extensions.BaseServerEventHandler

class UserLoginHandler : BaseServerEventHandler() {
    private val _envManager: IPvpEnvManager = PvpZoneExtension.AllServices.envManager
    private val _logger: ILogger = PvpZoneExtension.AllServices.logger
    private val _matchManager: IMatchManager = PvpZoneExtension.AllServices.matchManager

    private val _json = JsonUtility.json

    override fun handleServerEvent(event: ISFSEvent) {
        val loginName = event.getParameter(SFSEventParam.LOGIN_NAME) as String
        val data = event.getParameter(SFSEventParam.LOGIN_IN_DATA) as ISFSObject
        val infoObject = data.getSFSObject("info")
        val hash = data.getUtfString("hash")
        val infoClient = _json.decodeFromString<MatchInfoClient>(infoObject.toJson())
        //Convert biến kiểu _ từ client về biến kiểu camelCase, Sau này sửa client thì sẽ bỏ bước này
        val info = infoClient.toMatchInfo()
        validateLoginInfo(loginName, info, hash)
        val session = event.getParameter(SFSEventParam.SESSION) as ISession
        session.setProperty(MatchInfo.PROPERTY_KEY, info)
    }

    private fun validateLoginInfo(loginName: String, info: IMatchInfo, hash: String) {
        if (info.serverId != _envManager.serverId) {
            throw SFSLoginException("Invalid match server", SFSErrorData(ServerError.PVP_INVALID_MATCH_SERVER))
        }
        if (info.slot < info.info.size) {
            // Participant.
            val userInfo = info.info[info.slot]
            if (loginName != userInfo.username) {
                throw SFSLoginException("User name is incorrect")
            }
        } else {
            // Observer.
        }
        try {
            _matchManager.validate(info, hash)
        } catch (ex: InvalidMatchHashException) {
            throw SFSLoginException("Invalid match hash", SFSErrorData(ServerError.PVP_INVALID_MATCH_HASH))
        } catch (ex: MatchExpiredException) {
            throw SFSLoginException("Match expired", SFSErrorData(ServerError.PVP_MATCH_EXPIRED))
        } catch (ex: Exception) {
            _logger.log(ex.stackTraceToString())
            throw SFSLoginException("Internal error", SFSErrorData(ServerError.PVP_INTERNAL_ERROR))
        }
    }
}