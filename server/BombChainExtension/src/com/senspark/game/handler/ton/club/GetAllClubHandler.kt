package com.senspark.game.handler.ton.club

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.ton.IClubManager
import com.smartfoxserver.v2.entities.data.ISFSObject

class GetAllClubHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_ALL_CLUB_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (controller.dataType != EnumConstants.DataType.TON) {
            return sendError(controller, requestId, ErrorCode.NOT_USER_TON, null)
        }

        try {
            val response = controller.svServices.get<IClubManager>().getListClub()

            return sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            return sendExceptionError(controller, requestId, e)
        }
    }
}