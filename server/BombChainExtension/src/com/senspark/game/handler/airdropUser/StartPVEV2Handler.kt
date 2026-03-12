package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.KickReason
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.treasureHuntV2.ITreasureHuntV2Manager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class StartPVEV2Handler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.START_PVE_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.checkHash() || controller.userInfo.type == EnumConstants.UserType.TR) {
            controller.disconnect(KickReason.CHEAT_LOGIN)
            return
        }
        var result: ISFSObject = SFSObject()
        try {
            result = controller.masterUserManager.userBlockMapManager.getBombermanDangerous(controller)
            controller.svServices.get<ITreasureHuntV2Manager>().joinRoom(controller)

            result.putBool("is_trial", false)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
        return sendSuccess(controller, requestId, result)
    }
}