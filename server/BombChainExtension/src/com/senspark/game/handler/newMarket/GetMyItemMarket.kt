package com.senspark.game.handler.newMarket

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class GetMyItemMarket : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_MY_ITEM_MARKET_V3

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val response = controller.masterUserManager.userMarketplaceManager.getMyItemMarket()
            return sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    
    }
}