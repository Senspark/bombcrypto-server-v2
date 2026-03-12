package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class StopPVEV2Handler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.STOP_PVE_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        controller.setNeedSave(EnumConstants.SAVE.MAP)
//        treasureHuntV2Manager.leaveRoom(controller)
        return sendSuccess(controller, requestId, SFSObject())
    }

}