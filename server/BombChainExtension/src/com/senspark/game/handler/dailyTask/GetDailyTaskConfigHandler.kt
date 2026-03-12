package com.senspark.game.handler.dailyTask

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class GetDailyTaskConfigHandler() : BaseEncryptRequestHandler(
) {
    override val serverCommand = SFSCommand.GET_DAILY_TASK_CONFIG
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val todayTask = controller.masterUserManager.userDailyTaskManager.getUserTodayTask()
            val response = todayTask.toSfsObject()
            sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            return sendExceptionError(controller, requestId, e)
        }
    }
}