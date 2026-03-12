package com.senspark.game.handler.user

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class MarkItemViewedHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.MARK_ITEM_VIEWED_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val itemId = data.getInt("item_id")
            controller.masterUserManager.userOldItemManager.checkAndAddOldItem(itemId)
            sendSuccess(controller, requestId, SFSObject())
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}