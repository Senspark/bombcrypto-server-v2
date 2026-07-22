package com.senspark.game.manager.ton

import com.senspark.common.service.IServerService
import com.senspark.game.declare.EnumConstants
import com.smartfoxserver.v2.entities.data.SFSObject

interface IForceLoginManager : IServerService {
    fun checkToKickAccountFi(uid: Int, dataType: EnumConstants.DataType, landing: EnumConstants.Landing, forceLogin: Boolean)
}