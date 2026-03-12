package com.senspark.game.handler.ton.club

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.ton.IClubManager
import com.smartfoxserver.v2.entities.data.ISFSObject

class GetClubInfoHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_CLUB_INFO_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (controller.dataType != EnumConstants.DataType.TON) {
            return sendError(controller, requestId, ErrorCode.NOT_USER_TON, null)
        }

        try {
            val clubManager = controller.svServices.get<IClubManager>()
            val response: ISFSObject
            if (data.containsKey("club_id") || data.containsKey("id")) {
                var clubId = data.getInt("id")

                //Support old Ton client
                val clubIdLong = data.getLong("club_id")
                if (clubIdLong != null && clubIdLong > 0) {
                    if (clubIdLong > Int.MAX_VALUE) {
                        return sendError(controller, requestId, ErrorCode.SERVER_ERROR, "Club ID is out of range")
                    }
                    clubId = clubIdLong.toInt()
                }

                response = clubManager.getClubInfoById(clubId)
            } else {
                response = clubManager.getClubInfo(controller.userId)
            }

            return sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            return sendExceptionError(controller, requestId, e)
        }
    }
}