package com.senspark.game.handler.nft

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class SyncBombermanHandlerV4 : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.SYNC_BOMBERMAN_V4

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val forceFresh = data.containsKey(SFSField.ForceFresh) && data.getBool(SFSField.ForceFresh)
            val response: ISFSObject = controller.masterUserManager.heroFiManager.syncBomberManV4(forceFresh)
            return sendSuccess(controller, requestId, response)
        } catch (exception: Exception) {
            controller.logger.error("[SYNC_BOMBERMAN_V4]", exception)
            throw exception
        }
    }
}
