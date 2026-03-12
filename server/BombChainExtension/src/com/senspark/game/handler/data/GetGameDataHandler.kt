package com.senspark.game.handler.data

import com.senspark.common.pvp.IRankManager
import com.senspark.game.controller.LegacyUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.room.BaseGameRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetGameDataHandler : BaseGameRequestHandler() {
    override val serverCommand = SFSCommand.GET_GAME_DATA

    override fun handleGameClientRequest(controller: LegacyUserController, params: ISFSObject) {
        return try {
            val pvpRankManager = controller.svServices.get<IRankManager>()
            val result: ISFSObject = SFSObject().apply {
                putInt("bomb_rank", pvpRankManager.getRank(controller.pvpRank.point.value))
                putInt("current_point", controller.pvpRank.point.value)
            }
            sendResponseToClient(
                result, controller
            )
        } catch (e: Exception) {
            sendMessageError(e, controller)
        }
    }
}