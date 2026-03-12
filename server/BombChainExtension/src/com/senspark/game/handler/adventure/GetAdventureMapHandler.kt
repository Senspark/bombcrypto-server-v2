package com.senspark.game.handler.adventure

import com.senspark.game.constant.Booster
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.user.ITrGameplayManager
import com.smartfoxserver.v2.entities.data.ISFSObject

class GetAdventureMapHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_ADVENTURE_MAP_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val isPreview = data.getBool("is_preview") ?: false
            val trGameplayManager = services.get<ITrGameplayManager>()

            if(!isPreview) {
                if (trGameplayManager.isPlaying(controller.userId)) {
                    throw CustomException("Your account is already playing on other network")
                }
            }

            val manager = controller.masterUserManager.userAdventureModeManager
            val version = if (data.containsKey("version")) data.getInt("version") else 1
            val stage = data.getInt("stage")
            val level = data.getInt("level")
            val heroId = data.getInt("hero_id")
            val boosterList = data.getIntArray("boosters").toList()
            val boosters = boosterList.map { Booster.fromValue(it) }.toSet()
            val response = manager.getMap(version, heroId, stage, level, boosters)

            if(!isPreview)
            {
                // Add this user on the list playing adventure mode
                trGameplayManager.joinAdventure(controller.userId, controller.dataType)
            }

            return sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}