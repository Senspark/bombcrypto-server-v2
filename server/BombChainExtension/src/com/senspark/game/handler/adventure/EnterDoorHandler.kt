package com.senspark.game.handler.adventure

import com.senspark.game.controller.IUserController
import com.senspark.game.db.ILogDataAccess
import com.senspark.game.declare.KickReason
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.customEnum.BanReason
import com.senspark.game.exception.HackException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.dailyTask.DailyTaskManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class EnterDoorHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.ENTER_ADVENTURE_DOOR_V2

    private val logDataAccess = services.get<ILogDataAccess>()

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            controller.saveGameAndLoadReward()
            val triple = controller.masterUserManager.userAdventureModeManager.enterDoor()
            controller.loadReward()
            val sfsObject = SFSObject.newInstance().apply {
                putSFSArray("rewards", triple.second)
                putUtfString("reward_id", triple.first)
            }

            // Kiểm tra xem đây có phải mode đánh boss ko để complete daily task
            val isBossLevel = triple.third
            if(isBossLevel) {
                controller.masterUserManager.userDailyTaskManager.updateProgressTask(DailyTaskManager.DefeatBossInAdventure)
            }
            // Hoàn thành 1 level adventure mode, check và update daily task
            controller.masterUserManager.userDailyTaskManager.updateProgressTask(DailyTaskManager.PlayAdventure)
            
            return sendSuccess(controller, requestId, sfsObject)
        } catch (ex: HackException) {
            parseExceptionContents(ex, controller)
            controller.disconnect(KickReason.HACK_CHEAT)
            logDataAccess.logHack(
                controller.userName,
                11,
                ex.message ?: "Hack story mode"
            )
            if (ex.ban) {
                controller.ban(1, BanReason.ENTER_DOOR, null)
            }
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}