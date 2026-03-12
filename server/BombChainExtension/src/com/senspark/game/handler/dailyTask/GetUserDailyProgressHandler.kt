package com.senspark.game.handler.dailyTask

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetUserDailyProgressHandler() : BaseEncryptRequestHandler(
) {
    override val serverCommand = SFSCommand.GET_USER_DAILY_PROGRESS
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val listProgress = controller.masterUserManager.userDailyTaskManager.getUserDailyProgress()
            val response = SFSObject()
            response.putIntArray("progress", listProgress)
            sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            return sendExceptionError(controller, requestId, e)
        }
    }
}