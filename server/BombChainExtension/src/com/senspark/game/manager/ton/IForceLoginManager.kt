package com.senspark.game.manager.ton

import com.senspark.common.service.IServerService
import com.senspark.game.declare.EnumConstants
import com.smartfoxserver.v2.entities.data.SFSObject

interface IForceLoginManager : IServerService {
    fun checkToForceLogin(data: SFSObject, username: String, dataType: EnumConstants.DataType, sessionId: String?)
    fun checkToKickUser(username: String, sessionHash: String): Boolean
    fun checkToKickAccountFi(uid: Int, dataType: EnumConstants.DataType)
}