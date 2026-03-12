package com.senspark.game.handler.rock

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.rock.IBuyRockManager
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class UserBuyRockPackHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.BUY_ROCK_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val buyRockManager = controller.svServices.get<IBuyRockManager>()
            val rockPackage = buyRockManager.valueOf(data.getUtfString("package"))
                ?: throw Exception("Could not find package")
            val rewardType = BLOCK_REWARD_TYPE.valueOf(data.getInt("reward_type"))

            controller.masterUserManager.userBuyRockManager.buyPackage(rockPackage, rewardType)

            //Load lại rock
            val blockRewardManager = controller.masterUserManager.blockRewardManager
            blockRewardManager.loadUserBlockReward()

            val reward = SFSObject()
            reward.putInt("rock_received", rockPackage.getRockAmount())
            reward.putInt("rock_total", blockRewardManager.getTotalRockHaving().toInt())
            //Dùng để update các block reward đã thay đổi phía client
            reward.putSFSArray("rewards", blockRewardManager.toSfsArrays())
            sendSuccess(controller, requestId, reward)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}