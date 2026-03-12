package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class BuyHouseServerHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.BUY_HOUSE_SERVER
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.isAirdropUser()) {
            return sendError(controller, requestId, ErrorCode.NOT_USER_AIRDROP, null)
        }

        try {
            val rarity = data.getInt("rarity")
            val house = when {
                data.containsKey("reward_type") -> {
                    val rewardType = EnumConstants.BLOCK_REWARD_TYPE.valueOf(data.getInt("reward_type"))
                    val networkType = rewardType.convertDepositToNetworkType()
                    if(networkType == EnumConstants.DataType.UNKNOWN){
                        controller.masterUserManager.houseManager.buyHouseServer(rarity)
                    }
                    else{
                        controller.masterUserManager.houseManager.buyHouseServerWithTokenNetwork(networkType, rarity)
                    }
                }
                else -> {
                    // Chỉ có user SOL TON mới đc mua nhà bằng bcoin deposited, RON và BAS ko đc
                    if (controller.dataType == EnumConstants.DataType.TON || 
                        controller.dataType == EnumConstants.DataType.SOL) {
                        controller.masterUserManager.houseManager.buyHouseServer(rarity)
                    } else {
                        return sendError(controller, requestId, ErrorCode.NOT_SUPPORTED, null)
                    }
                }
            }
            controller.masterUserManager.blockRewardManager.loadUserBlockReward()

            val response = SFSObject()
            response.putUtfString(SFSField.House_Gen_Id, house.details.details)
            response.putInt(SFSField.Active, if (house.isActive) 1 else 0)
            response.putSFSArray(SFSField.Rewards, controller.masterUserManager.blockRewardManager.toSfsArrays())
            return sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            return sendExceptionError(controller, requestId, e)
        }
    }
}