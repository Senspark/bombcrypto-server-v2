package com.senspark.game.handler.shop

import com.senspark.game.constant.ItemPackage
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class BuyCostumeItemHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.BUY_COSTUME_ITEM_V2
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val manager = controller.masterUserManager.userInventoryManager
            val itemId = data.getInt("item_id")
            val quantity = data.getInt("quantity")
            val itemPackage = ItemPackage.valueOf(data.getUtfString("item_package"))
            manager.buyCostumeItem(itemId, itemPackage, quantity)
            sendSuccess(controller, requestId, SFSObject())
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}