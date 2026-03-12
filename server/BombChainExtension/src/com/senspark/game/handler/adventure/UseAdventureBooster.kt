package com.senspark.game.handler.adventure

import com.senspark.game.constant.Booster
import com.senspark.game.controller.IUserController
import com.senspark.game.db.ILogDataAccess
import com.senspark.game.declare.KickReason
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class UseAdventureBoosterHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.USE_ADVENTURE_BOOSTER_V2

    private val logDataAccess = services.get<ILogDataAccess>()

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val manager = controller.masterUserManager.userAdventureModeManager
            val booster = Booster.fromValue(data.getInt("booster"))
            manager.useBooster(booster)
            return sendSuccess(controller, requestId, SFSObject())
        } catch (ex: Exception) {
            controller.disconnect(KickReason.HACK_CHEAT)
            logDataAccess.logHack(
                controller.userName,
                11,
                "Hack story mode use invalid booster"
            )
        }
    }
}