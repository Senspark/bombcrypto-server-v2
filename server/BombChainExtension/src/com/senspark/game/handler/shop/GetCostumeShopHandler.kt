package com.senspark.game.handler.shop

import com.senspark.common.utils.toSFSArray
import com.senspark.game.constant.ItemType
import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetCostumeShopHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_COSTUME_SHOP_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val configItemManager = controller.svServices.get<IConfigItemManager>()
            val itemType = ItemType.fromValue(data.getInt("item_type"))
            val result = configItemManager.getItems(itemType).filter { it.isSaleOnShop }.sortedBy { it.sortIndex }
                .toSFSArray { it.toSFSObjectForCostumeShop() }
            val response = SFSObject().apply { putSFSArray("data", result) }
            return sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }

}