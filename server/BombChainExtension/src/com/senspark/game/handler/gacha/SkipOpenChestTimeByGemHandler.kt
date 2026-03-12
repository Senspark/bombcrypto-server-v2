package com.senspark.game.handler.gacha

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.KickReason
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class SkipOpenChestTimeByGemHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.SKIP_OPEN_CHEST_TIME_BY_GEM_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.checkHash()) {
            controller.disconnect(KickReason.CHEAT_LOGIN)
            throw Exception("Invalid hash")
        }
        return try {
            val chestId = data.getInt("chest_id")
            controller.masterUserManager.userGachaChestManager.skipOpenTimeByGem(chestId)
            sendSuccess(controller, requestId, data)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}