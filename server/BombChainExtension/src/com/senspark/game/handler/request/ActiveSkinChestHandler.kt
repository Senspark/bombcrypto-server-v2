package com.senspark.game.handler.skinPVP

import com.senspark.common.constant.ExpirationAfter
import com.senspark.common.constant.ItemId
import com.senspark.game.constant.ItemType
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

open class ActiveSkinChestHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.ACTIVE_SKIN_CHEST_V3

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val response = SFSObject()
        try {
            val manager = controller.masterUserManager.userInventoryManager
            val itemType = ItemType.fromValue(data.getInt("item_type"))
            val sfsArray = data.getSFSArray("items")
            val items = (0 until sfsArray.size()).associateBy({
                sfsArray.getSFSObject(it).getInt("item_id") as ItemId
            }) {
                val obj = sfsArray.getSFSObject(it)
                (if (obj.containsKey("expiration_after")) obj.getLong("expiration_after") else null) as ExpirationAfter
            }
            manager.activeSkinChest(itemType, items)
        } catch (ex: Exception) {
            return sendExceptionError(controller, requestId, ex)
        }
        return sendSuccess(controller, requestId, response)
    }
}