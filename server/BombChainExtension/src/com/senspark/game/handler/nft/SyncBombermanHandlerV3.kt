package com.senspark.game.handler.nft

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.KickReason
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class SyncBombermanHandlerV3 : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.SYNC_BOMBERMAN_V3

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.checkHash()) {
            controller.disconnect(KickReason.CHEAT_LOGIN)
            return
        }
        return try {
            val response: ISFSObject = controller.masterUserManager.heroFiManager.syncBomberManV3()
            return sendSuccess(controller, requestId, response)
        } catch (exception: Exception) {
            controller.logger.error("[SYNC_BOMBERMAN_V3]", exception)
            throw exception
        }
    }
}

