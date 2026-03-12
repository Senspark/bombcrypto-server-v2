package com.senspark.game.handler.ton.club

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.ton.IClubManager
import com.smartfoxserver.v2.entities.data.ISFSObject

class UserJoinClubHandlerV3 : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.JOIN_CLUB_V3

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (controller.dataType != EnumConstants.DataType.TON) {
            return sendError(controller, requestId, ErrorCode.NOT_USER_TON, null)
        }

        try {
            val clubManager = controller.svServices.get<IClubManager>()
            val clubName = data.getUtfString("club_name")
            clubManager.joinClubV3(controller.userId, clubName)
            val response = clubManager.getClubInfo(controller.userId)
            return sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            if (e is CustomException) {
                return sendExceptionError(controller, requestId, e)
            }
            return sendExceptionError(
                controller, requestId,
                CustomException("Club does not exist \\n Please try again")
            )
        }
    }
}
