package com.senspark.game.handler.ton.club

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.ton.IClubManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class UserLeaveClubHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.LEAVE_CLUB_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (controller.dataType != EnumConstants.DataType.TON) {
            return sendError(controller, requestId, ErrorCode.NOT_USER_TON, null)
        }

        try {
            controller.svServices.get<IClubManager>().leaveClub(controller.userId)

            return sendSuccess(controller, requestId, SFSObject())
        } catch (e: Exception) {
            return sendExceptionError(controller, requestId, e)
        }
    }
}