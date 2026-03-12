package com.senspark.game.handler.upgradeHero

import com.senspark.game.constant.ItemStatus
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.dailyTask.DailyTaskManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GrindHeroHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GRIND_HEROES_V2
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            controller.saveGameAndLoadReward()
            val manager = controller.masterUserManager.heroTRManager
            val itemId = data.getInt("item_id")
            val quantity = data.getInt("quantity")
            val itemStatus = ItemStatus.fromValue(data.getInt("status"))
            val result = manager.grindHero(itemId, quantity, itemStatus)

            // Hoàn thành 1 grind 1 hero, check và update daily task
            controller.masterUserManager.userDailyTaskManager.updateProgressTask(DailyTaskManager.GrindHero)
            
            val response = SFSObject().apply {
                putSFSArray("data", result)
            }
            sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}