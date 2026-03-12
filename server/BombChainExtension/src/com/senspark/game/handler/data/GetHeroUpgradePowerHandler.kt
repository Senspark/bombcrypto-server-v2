package com.senspark.game.handler.heroTR

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.hero.IHeroUpgradePowerManager
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class GetHeroUpgradePowerHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_HERO_UPGRADE_POWER_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val heroUpgradePowerManager = controller.svServices.get<IHeroUpgradePowerManager>()
        val response: ISFSObject = heroUpgradePowerManager.toSFSObject()
        return sendSuccess(controller, requestId, response)
    }
}