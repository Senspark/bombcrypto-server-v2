package com.senspark.game.handler.adventure

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.adventure.IAdventureLevelConfigManager
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.user.ITrGameplayManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetAdventureLevelHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_ADVENTURE_LEVEL_DETAIL_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val manager = controller.masterUserManager.userAdventureModeManager
            val configManager = controller.svServices.get<IAdventureLevelConfigManager>()
            manager.clearOldMap()
            val userAdventureMode = manager.userAdventureMode
            val response: ISFSObject = SFSObject()
            response.putInt("current_level", userAdventureMode.currentLevel)
            response.putInt("current_stage", userAdventureMode.currentStage)
            response.putInt("max_level", userAdventureMode.maxLevel)
            response.putInt("max_stage", userAdventureMode.maxStage)
            response.putBool("is_new", true)
            response.putInt("hero_id", userAdventureMode.heroId)
            response.putSFSArray("level_map", configManager.getLevelMap())

            // Remove this user from the list playing adventure mode
            val trGameplayManager = services.get<ITrGameplayManager>()
            trGameplayManager.leaveAdventure(controller.userId, controller.dataType)

            return sendSuccess(controller, requestId, response)
        }
        catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}