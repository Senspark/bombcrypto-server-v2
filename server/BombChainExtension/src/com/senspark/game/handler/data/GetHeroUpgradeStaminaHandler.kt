package com.senspark.game.handler.heroTR

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.hero.IHeroUpgradeStaminaManager
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class GetHeroUpgradeStaminaHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_HERO_UPGRADE_STAMINA_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val heroUpgradeStaminaManager = controller.svServices.get<IHeroUpgradeStaminaManager>()
        val response: ISFSObject = heroUpgradeStaminaManager.toSFSObject()
        return sendSuccess(controller, requestId, response)
    }
}
