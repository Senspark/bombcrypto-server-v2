package com.senspark.game.handler.upgradeHero

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.customEnum.UpgradeHeroType
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.dailyTask.DailyTaskManager
import com.smartfoxserver.v2.entities.data.ISFSObject

class UpgradeHeroTrHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.UPGRADE_HERO_TR_V2
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            controller.saveGameAndLoadReward()
            val manager = controller.masterUserManager.heroTRManager
            val heroId = data.getInt("hero_id")
            val type = UpgradeHeroType.valueOf(data.getUtfString("type"))
            val response = manager.upgradeHero(heroId, type)

            // Hoàn thành upgrade hero, check và update daily task
            controller.masterUserManager.userDailyTaskManager.updateProgressTask(DailyTaskManager.UpgradeHero)
            sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}