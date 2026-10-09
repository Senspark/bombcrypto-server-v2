package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

// Client paused treasure mode (dialog, settings...): heroes halt until RESUME_TREASURE_MODE; live bombs still explode.
class PauseTreasureModeHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.PAUSE_TREASURE_MODE

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        scheduler.fireAndForget {
            controller.masterUserManager.userBlockMapManagerV2.setTreasurePaused(true)
            sendSuccess(controller, requestId, SFSObject())
        }
    }
}
