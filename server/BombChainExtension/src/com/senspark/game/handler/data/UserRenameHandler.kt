package com.senspark.game.handler.data

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class UserRenameHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.USER_RENAME_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        throw NotImplementedError()
//        var name = ""
//        try {
//            name = params.getUtfString("name").trim()
//            val feeRewardType = valueOf(params.getInt("reward_type"))
//            if (!setOf(BCOIN, SENSPARK).contains(feeRewardType)) {
//                return sendMessageError(
//                    CustomException("Reward ${feeRewardType.name} invalid", ErrorCode.INVALID_PARAMETER),
//                    controller
//                )
//            }
//            if (!nameIsValid(name)) {
//                return sendMessageError(
//                    CustomException("Name in 4,10 characters", ErrorCode.INVALID_PARAMETER),
//                    controller
//                )
//            }
//            controller.rename(name, feeRewardType)
//
//            val result = SFSObject()
//            result.putSFSArray("rewards", controller.masterUserManager.blockRewardManager.toSfsArrays())
//            return sendResponseToClient(result, controller)
//        } catch (sqlException: SQLException) {
////            trùng tên
//            return if (sqlException.errorCode == 1062)
//                sendMessageError(
//                    CustomException("The name \"$name\" has been used", ErrorCode.INVALID_PARAMETER),
//                    controller
//                )
//            else sendMessageError(sqlException, controller)
//        } catch (ex: Exception) {
//            return sendMessageError(ex, controller)
//        }
    }

    private fun nameIsValid(name: String): Boolean {
        val length = name.length
        return length in 4..10
    }
}