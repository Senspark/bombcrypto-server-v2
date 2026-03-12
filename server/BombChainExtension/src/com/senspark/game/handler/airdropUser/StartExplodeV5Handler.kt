package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.*
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import com.smartfoxserver.v2.extensions.ExtensionLogLevel

class StartExplodeV5Handler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.START_EXPLODE_V5

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        scheduler.fireAndForget {
            doWork(controller, requestId, data)
        }
    }

    private fun doWork(controller: IUserController, requestId: Int, data: ISFSObject) {
        val heroId = data.getInt("id")
        val bombNo = data.getInt("num")
        val colBoom = data.getInt("i")
        val rowBoom = data.getInt("j")

        val bbmController = controller.masterUserManager.heroFiManager
        val bbm = bbmController.getHero(heroId, controller.dataType)

        if (bbm == null || bbm.details.dataType != controller.dataType) {
            controller.logger.warn("StartExplodeV4Handler: Bomber man null genid:userId = ${heroId} ${controller.userName}")
            return sendError(controller, requestId, ErrorCode.BOMBERMAN_NULL, null)
        }
        if (bbm.details.dataType != controller.dataType) {
            controller.logger.warn("ATTENTION WRONG HERO, ${controller.userName} ${controller.dataType}, ${bbm.heroId} ${bbm.details.dataType}")
            return sendError(controller, requestId, ErrorCode.BOMBERMAN_NULL, null)
        }
        if (!bbm.isActive) {
            return sendError(controller, requestId, ErrorCode.BOMBERMAN_ACTIVE_INVALID, null)
        }
        if (bbm.stage != GameConstants.BOMBER_STAGE.WORK) {
            return sendError(controller, requestId, ErrorCode.BOMBERMAN_IS_NOT_WORKING, null)
        }

        // kiểm tra thể lực. còn thì trừ đi khong thì báo lỗi
        if (bbm.energy <= 0) {
            val resultData: ISFSObject = SFSObject()
            resultData.putLong(SFSField.ID, bbm.heroId.toLong())
            resultData.putInt(SFSField.Energy, bbm.energy)
            val blocksResult: ISFSArray = SFSArray()
            resultData.putSFSArray(SFSField.Blocks, blocksResult)
            val attendPools = listOf<Int>()
            resultData.putIntArray("attend_pools", attendPools)
            resultData.putInt(SFSField.HeroType, bbm.type.value)
            return sendSuccess(controller, requestId, resultData)
        }

        val blockMap = controller.masterUserManager.userBlockMapManager

        synchronized(blockMap.locker) {
            if (blockMap.checkHackSpeedBombExplode(bbm, bombNo)) {
                return sendError(controller, requestId, ErrorCode.HACK_SPEED, null)
            }

            //kiểm tra vị trí đặt boom xem phải chỗ trống không
            if (!blockMap.canSetBoom(colBoom, rowBoom)) {
                return sendError(controller, requestId, ErrorCode.STARTEXPLODE_CAN_NOT_SET_BOOM, null)
            }

            //Check hack bomb co ban
            val blockArr = data.getSFSArray(SFSField.Blocks)
            if (blockMap.checkHackExplodeBlock(bbm, blockArr)) {
                return sendError(controller, requestId, ErrorCode.HACK_EXPLODE_BLOCK, null)
            }

            val result = blockMap.explode(bbm, colBoom, rowBoom, blockArr)
            controller.setNeedSave(EnumConstants.SAVE.HERO_STATUS)
            controller.setNeedSave(EnumConstants.SAVE.MAP)
            return sendSuccess(controller, requestId, result)
        }
    }
}