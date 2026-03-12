package com.senspark.game.handler.heroTR

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class ActiveHeroTRHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.ACTIVE_HERO_TR_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val heroId = data.getInt("hero_id")
            controller.masterUserManager.heroTRManager.active(heroId)
            sendSuccess(controller, requestId, SFSObject())
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}