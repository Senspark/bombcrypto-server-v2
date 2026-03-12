package com.senspark.game.handler.data

import com.senspark.game.api.IAuthApi
import com.senspark.game.controller.IUserController
import com.senspark.game.controller.LegacyUserController
import com.senspark.game.declare.KickReason
import com.senspark.game.declare.SFSCommand.DELETE_USER_V2
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class DeleteUserHandler : BaseEncryptRequestHandler() {

    override val serverCommand = DELETE_USER_V2

    private val _authApi = services.get<IAuthApi>()

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        controller as LegacyUserController
        try {
            val accessToken = data.getUtfString("access_token")
            val deleteRecord = _authApi.deleteUser(controller.userId, accessToken)
            if (deleteRecord) {
                controller.deleteAccount()
            }
            controller.disconnect(KickReason.USER_DELETED)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}