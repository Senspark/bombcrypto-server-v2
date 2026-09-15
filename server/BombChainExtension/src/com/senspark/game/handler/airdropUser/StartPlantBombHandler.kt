package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.*
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.blockMap.PlantBombResult
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

// Client plants at its assigned target; returns the next target (see server/docs/explode_v6.md).
class StartPlantBombHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.START_PLANT_BOMB

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        scheduler.fireAndForget {
            try {
                doWork(controller, requestId, data)
            } catch (e: CustomException) {
                sendExceptionError(controller, requestId, e)
            }
        }
    }

    private fun doWork(controller: IUserController, requestId: Int, data: ISFSObject) {
        val heroId = data.getInt("id")
        val bombNo = data.getInt("num")
        val col = data.getInt("i")
        val row = data.getInt("j")

        controller.logger.log("[EXPLODE_V2] StartPlantBomb REQ hero=$heroId bombNo=$bombNo pos=($col,$row) user=${controller.userName}")

        val bbmController = controller.masterUserManager.heroFiManager
        val bbm = bbmController.getHero(heroId, controller.dataType)

        if (bbm == null || bbm.details.dataType != controller.dataType) {
            controller.logger.warn("StartPlantBombHandler: Bomber man null genid:userId = ${heroId} ${controller.userName}")
            return sendError(controller, requestId, ErrorCode.BOMBERMAN_NULL, null)
        }
        if (!bbm.isActive) {
            controller.logger.log("[EXPLODE_V2] StartPlantBomb REJECT hero=$heroId reason=not_active")
            return sendError(controller, requestId, ErrorCode.BOMBERMAN_ACTIVE_INVALID, null)
        }
        if (bbm.stage != GameConstants.BOMBER_STAGE.WORK) {
            controller.logger.log("[EXPLODE_V2] StartPlantBomb REJECT hero=$heroId reason=not_working stage=${bbm.stage}")
            return sendError(controller, requestId, ErrorCode.BOMBERMAN_IS_NOT_WORKING, null)
        }

        val blockMap = controller.masterUserManager.userBlockMapManagerV2

        synchronized(blockMap.locker) {
            val outcome = blockMap.plantBomb(heroId, bombNo, col, row, bbm.speed, bbm.bombCount)
            when (outcome.result) {
                PlantBombResult.TARGET_MISMATCH -> {
                    controller.logger.log("[EXPLODE_V2] StartPlantBomb MISMATCH hero=$heroId bombNo=$bombNo requestedPos=($col,$row)")
                    return sendError(controller, requestId, ErrorCode.PLANT_TARGET_MISMATCH, null)
                }

                PlantBombResult.TOO_FAST -> {
                    controller.logger.log("[EXPLODE_V2] StartPlantBomb TOO_FAST hero=$heroId bombNo=$bombNo requestedPos=($col,$row)")
                    return sendError(controller, requestId, ErrorCode.PLANT_TOO_FAST, null)
                }

                PlantBombResult.NO_BOMB_TO_PLANT -> {
                    // All bombs live; client waits for a RESPONSE_EXPLODE before retrying.
                    controller.logger.log("[EXPLODE_V2] StartPlantBomb NO_BOMB_TO_PLANT hero=$heroId bombNo=$bombNo requestedPos=($col,$row) bombCount=${bbm.bombCount}")
                    return sendError(controller, requestId, ErrorCode.NO_BOMB_TO_PLANT, null)
                }

                PlantBombResult.OK -> Unit
            }

            val nextTarget = outcome.nextTarget
            if (nextTarget == null) {
                controller.logger.log("[EXPLODE_V2] StartPlantBomb NO_NEXT_TARGET hero=$heroId bombNo=$bombNo plantedPos=($col,$row)")
                return sendError(controller, requestId, ErrorCode.NO_BOMB_TARGET, null)
            }

            controller.logger.log("[EXPLODE_V2] StartPlantBomb OK hero=$heroId bombNo=$bombNo plantedPos=($col,$row) nextTarget=${nextTarget.first},${nextTarget.second}")

            val resultData: ISFSObject = SFSObject()
            resultData.putLong(SFSField.ID, bbm.heroId.toLong())
            resultData.putInt(SFSField.HeroType, bbm.type.value)
            resultData.putInt("i", nextTarget.first)
            resultData.putInt("j", nextTarget.second)
            return sendSuccess(controller, requestId, resultData)
        }
    }
}
