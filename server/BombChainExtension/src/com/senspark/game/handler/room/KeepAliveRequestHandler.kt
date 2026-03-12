package com.senspark.game.handler.room

import com.senspark.common.service.IServerService
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.handler.MainGameExtensionBaseRequestHandler
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.IUsersManager
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSObject

class KeepAliveRequestHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.PING_PONG

    override fun handleGameClientRequest(
        controller: IUserController,
        requestId: Int,
        data: ISFSObject
    ) {
        // Get the UsersManager directly from services
        val usersManager = controller.svServices.get<IUsersManager>()
        val userInfo = controller.userInfo
        if (userInfo != null) {
            usersManager.updateKeepAliveTime(controller.userId, userInfo.dataType)
        }
        
    }
}
