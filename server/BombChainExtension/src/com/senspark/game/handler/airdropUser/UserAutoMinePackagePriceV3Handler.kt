package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class UserAutoMinePackagePriceV3Handler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.AUTO_MINE_PRICE_V3

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val response =
                if (controller.isAirdropUser()) {
                    controller.masterUserManager.userAutoMineManager.packagePriceUserAirdropV3(controller.dataType)
                } else {
                    controller.masterUserManager.userAutoMineManager.packagePriceV3()
                }
            sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}
