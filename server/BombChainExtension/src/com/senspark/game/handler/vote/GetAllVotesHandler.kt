package com.senspark.game.handler.vote

import com.senspark.game.controller.LegacyUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.room.BaseGameRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class GetAllVotesHandler : BaseGameRequestHandler() {
    override val serverCommand = SFSCommand.GET_ALL_VOTES

    override fun handleGameClientRequest(controller: LegacyUserController, params: ISFSObject) {
        TODO("Not yet implemented")
    }
}