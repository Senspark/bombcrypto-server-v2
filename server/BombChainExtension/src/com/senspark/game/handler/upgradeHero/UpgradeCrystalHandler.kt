package com.senspark.game.handler.upgradeHero

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class UpgradeCrystalHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.UPGRADE_CRYSTAL_V2
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            controller.saveGameAndLoadReward()
            val manager = controller.masterUserManager.userMaterialManager
            val itemId = data.getInt("item_id")
            val quantity = data.getInt("quantity")
            val response = manager.upgradeCrystal(itemId, quantity)
            sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}