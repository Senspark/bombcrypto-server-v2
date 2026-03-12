package com.senspark.game.handler.heroTR

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.grindHero.IGrindHeroManager
import com.senspark.game.data.manager.upgradeHero.IUpgradeCrystalManager
import com.senspark.game.data.manager.upgradeHero.IUpgradeHeroTrManager
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetUpgradeConfigHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_UPGRADE_CONFIG_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val grindHeroManager = controller.svServices.get<IGrindHeroManager>()
            val upgradeCrystalManager = controller.svServices.get<IUpgradeCrystalManager>()
            val upgradeHeroTrManager = controller.svServices.get<IUpgradeHeroTrManager>()

            val result: ISFSObject = SFSObject().apply {
                putSFSArray("grind_config", grindHeroManager.toSfsArray())
                putSFSArray("upgrade_crystal_config", upgradeCrystalManager.toSfsArray())
                putSFSArray("upgrade_hero_config", upgradeHeroTrManager.toSfsArray())
            }
            sendSuccess(controller, requestId, result)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}