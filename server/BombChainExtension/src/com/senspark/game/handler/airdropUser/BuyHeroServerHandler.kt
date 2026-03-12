package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class BuyHeroServerHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.BUY_HERO_SERVER
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.isAirdropUser()) {
            return sendError(controller, requestId, ErrorCode.NOT_USER_AIRDROP, null)
        }

        try {
            val quantity = data.getInt("quantity")
            val rewardType = BLOCK_REWARD_TYPE.valueOf(data.getInt("reward_type"))
            
            
            val sfsArray = if (rewardType == BLOCK_REWARD_TYPE.BOMBERMAN) {
                controller.masterUserManager.heroFiManager.claimHeroServer(quantity)
            } else {
                // Để đảm bảo starcore đào của user đồng bộ với db thì trước khi xử lý mua cần save game trước
                controller.saveGameAndLoadReward()
                
                controller.masterUserManager.heroFiManager.buyHeroServer(quantity, rewardType)
            }
            controller.masterUserManager.blockRewardManager.loadUserBlockReward()

            val response = SFSObject()
            response.putSFSArray(SFSField.Bombers, sfsArray)
            response.putSFSArray(SFSField.Rewards, controller.masterUserManager.blockRewardManager.toSfsArrays())
            return sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            return sendExceptionError(controller, requestId, e)
        }
    }
}