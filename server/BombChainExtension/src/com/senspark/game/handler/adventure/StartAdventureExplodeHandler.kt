package com.senspark.game.handler.adventure

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand.START_STORY_EXPLODE_V2
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class StartAdventureExplodeHandler : BaseEncryptRequestHandler() {
    override val serverCommand = START_STORY_EXPLODE_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val storyMapManager = controller.masterUserManager.userAdventureModeManager.matchManager
            val timestamp = data.getLong("bombId")
            val clientBlocks = data.getSFSArray(SFSField.Blocks)
            val response = storyMapManager.explode(timestamp, clientBlocks)
            response.putLong("bombId", timestamp)
            return sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }

    }
}