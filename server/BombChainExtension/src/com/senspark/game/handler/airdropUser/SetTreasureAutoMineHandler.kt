package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

// Client switched auto mine on/off: decides whether an exhausted hero goes home or just sleeps.
class SetTreasureAutoMineHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.SET_TREASURE_AUTO_MINE

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        scheduler.fireAndForget {
            val enabled = data.containsKey("auto_mine") && data.getBool("auto_mine") &&
                controller.masterUserManager.userAutoMineManager.isActive
            controller.masterUserManager.userBlockMapManagerV2.setTreasureAutoMine(enabled)
            sendSuccess(controller, requestId, SFSObject())
        }
    }
}
