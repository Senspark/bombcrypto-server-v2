package com.senspark.game.handler.gacha

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class BuyGachaChestSlotHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.BUY_GACHA_CHEST_SLOT_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val slot = data.getInt("slotNumber")
            controller.saveGameAndLoadReward()
            controller.masterUserManager.userConfigManager.buyGachaChestSlot(slot)
            controller.masterUserManager.blockRewardManager.loadUserBlockReward()
            sendSuccess(controller, requestId, SFSObject())
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }

    }
}