package com.senspark.game.handler.gacha

import com.senspark.common.utils.toSFSArray
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.user.IGachaChestManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetGachaChestShopHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_GACHA_CHEST_SHOP_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val gachaChestManager = controller.svServices.get<IGachaChestManager>()
            val shopConfig = gachaChestManager.chestList
            val response = SFSObject().apply {
                putSFSArray("data", shopConfig.toSFSArray { it.toSFSObject() })
            }
            return sendSuccess(controller, requestId, response)
        }
        catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}