package com.senspark.game.handler.onBoarding

import com.senspark.common.utils.toSFSArray
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class GetOnBoardingConfigHandler() : BaseEncryptRequestHandler(
) {
    override val serverCommand = SFSCommand.GET_ON_BOARDING_CONFIG
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (controller.isAirdropUser()) {
            return sendError(controller, requestId, ErrorCode.NOT_SUPPORT_AIRDROP_USER, null)
        }
        try {
            val response = SFSObject()
            val sfsArray = SFSArray()
            controller.masterUserManager.userOnBoardingManager.getConfig().forEach{
                val sfsObject = SFSObject()
                sfsObject.putInt("id", it.key)
                sfsObject.putFloat("reward", it.value)
                sfsArray.addSFSObject(sfsObject)
            }
            val currentStep = controller.masterUserManager.userOnBoardingManager.getUserProgress(controller.userId)
            response.putSFSArray("config", sfsArray)
            response.putInt("current_step", currentStep)
            return sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            return sendExceptionError(controller, requestId, e)
        }
    }
}