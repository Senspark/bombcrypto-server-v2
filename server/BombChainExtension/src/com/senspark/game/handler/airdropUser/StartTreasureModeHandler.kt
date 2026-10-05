package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.KickReason
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.treasureHuntV2.ITreasureHuntV2Manager
import com.smartfoxserver.v2.entities.data.ISFSObject

// The only call a server-driven treasure client makes: START_PVE_V2 + map + auto play in one
// (see server/docs/treasure_server_driven.md). Calling it again is a full resync.
class StartTreasureModeHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.START_TREASURE_MODE

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.checkHash() || controller.userInfo.type == EnumConstants.UserType.TR) {
            controller.disconnect(KickReason.CHEAT_LOGIN)
            return
        }
        scheduler.fireAndForget {
            try {
                // May put dangerous heroes to sleep, so it runs before the roster is picked.
                val dangerous = controller.masterUserManager.userBlockMapManager.getBombermanDangerous(controller)
                controller.svServices.get<ITreasureHuntV2Manager>().joinRoom(controller)

                val paused = data.containsKey("paused") && data.getBool("paused")
                val result = controller.masterUserManager.userBlockMapManagerV2.startTreasureMode(paused)
                result.putSFSArray("dangerous", dangerous.getSFSArray("dangerous"))
                result.putBool("is_trial", false)
                sendSuccess(controller, requestId, result)
            } catch (e: Exception) {
                sendExceptionError(controller, requestId, e)
            }
        }
    }
}
