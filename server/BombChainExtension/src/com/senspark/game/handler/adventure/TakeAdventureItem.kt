package com.senspark.game.handler.adventure

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class TakeAdventureItemHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.TAKE_ADVENTURE_ITEM_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val timestamp = data.getLong("timestamp")
        return try {
            val i = data.getInt("i")
            val j = data.getInt("j")
            if (timestamp == 0L) {
                throw CustomException("Invalid timestamp $timestamp")
            }
            val response = controller.masterUserManager.userAdventureModeManager.takeItem(i, j)
            response.putLong("timestamp", timestamp)
            return sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}