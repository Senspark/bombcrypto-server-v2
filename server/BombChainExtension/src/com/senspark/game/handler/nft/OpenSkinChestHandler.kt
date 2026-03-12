package com.senspark.game.handler.skinPVP

import com.senspark.common.utils.sfsArrayOf
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class OpenSkinChestHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.OPEN_SKIN_CHEST_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val response = SFSObject()
        try {
            val item = controller.openSkinChest()
            response.putSFSArray("data", sfsArrayOf(item.toSFSObject()))

        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }

        return sendSuccess(controller, requestId, response)
    }
}