package com.senspark.game.handler.pve

import com.senspark.game.controller.LegacyUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.room.BaseGameRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class ChangeMiningModeHandler : BaseGameRequestHandler() {
    override val serverCommand = SFSCommand.CHANGE_MINING_MODE

    override fun handleGameClientRequest(controller: LegacyUserController, params: ISFSObject) {
        return try {
            controller.saveGameAndLoadReward()
            val tokenType = EnumConstants.TokenType.valueOf(params.getUtfString(SFSField.TOKEN_TYPE))
            controller.masterUserManager.userMiningModeManager.changeMiningMode(tokenType)
            val result: ISFSObject = SFSObject()
            result.putUtfString(
                SFSField.TOKEN_TYPE, controller.masterUserManager.userMiningModeManager.miningMode.name
            )
            sendResponseToClient(result, controller)
        } catch (ex: Exception) {
            sendMessageError(ex, controller)
        }
    }
}