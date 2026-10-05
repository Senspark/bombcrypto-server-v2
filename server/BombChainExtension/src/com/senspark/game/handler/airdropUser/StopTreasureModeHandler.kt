package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

// Client left treasure mode: heroes stop; bombs already planted still explode and are credited.
class StopTreasureModeHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.STOP_TREASURE_MODE

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        scheduler.fireAndForget {
            controller.masterUserManager.userBlockMapManagerV2.stopTreasureMode()
            controller.setNeedSave(EnumConstants.SAVE.MAP)
            sendSuccess(controller, requestId, SFSObject())
        }
    }
}
