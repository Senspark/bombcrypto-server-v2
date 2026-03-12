package com.senspark.game.handler.dailyMission

import com.senspark.common.utils.toSFSArray
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.customEnum.MissionType
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetDailyMissionHandler : GetMissionHandler(SFSCommand.GET_DAILY_MISSION_V2)
class GetQuestsHandler : GetMissionHandler(SFSCommand.GET_QUESTS_V2)
class GetAchievementHandler : GetMissionHandler(SFSCommand.GET_ACHIEVEMENT_V2)

open class GetMissionHandler(
    override val serverCommand: String
) : BaseEncryptRequestHandler() {

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val type = when (serverCommand) {
                SFSCommand.GET_DAILY_MISSION_V2 -> MissionType.DAILY_GIFT
                SFSCommand.GET_QUESTS_V2 -> MissionType.DAILY_QUEST
                SFSCommand.GET_ACHIEVEMENT_V2 -> MissionType.ACHIEVEMENT
                else -> throw CustomException("Invalid serverCommand $serverCommand")
            }

            val manager = controller.masterUserManager.userMissionManager
            val result = manager.getMissions(type).sortedBy { it.sort }.toSFSArray {
                it.toSfsObject()
            }
            val response = SFSObject().apply { putSFSArray("data", result) }
            sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}