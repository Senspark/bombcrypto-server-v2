package com.senspark.game.handler.ton

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.ton.ITasksManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class CompleteTaskHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.COMPLETE_TASK_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (controller.dataType != EnumConstants.DataType.TON) {
            return sendError(controller, requestId, ErrorCode.NOT_USER_TON, null)
        }

        try {
            val taskId = data.getInt("task_id")
            val isCompleted = controller.svServices.get<ITasksManager>().completeTask(controller, taskId)

            val response = SFSObject()
            response.putBool("is_complete", isCompleted)
            return sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            return sendExceptionError(controller, requestId, e)
        }
    }
}