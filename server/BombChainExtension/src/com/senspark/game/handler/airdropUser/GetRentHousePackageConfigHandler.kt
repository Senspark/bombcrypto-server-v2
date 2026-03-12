package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.treassureHunt.IHouseManager
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetRentHousePackageConfigHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_RENT_HOUSE_PACKAGE_CONFIG
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.isAirdropUser()) {
            return sendError(controller, requestId, ErrorCode.NOT_USER_AIRDROP, null)
        }
        val houseManager = controller.svServices.get<IHouseManager>()
        return try {
            val response = SFSObject().apply { 
                putSFSArray("data", houseManager.packagePrice(controller.dataType))
            }

            sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}