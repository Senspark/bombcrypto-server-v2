package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.autoMine.IAutoMineManager
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class UserBuyAutoMineV2Handler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.BUY_AUTO_MINE_V2

    private val autoMineManager: IAutoMineManager = services.get<IAutoMineManager>()

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val packageAutoMine = autoMineManager.valueOf(data.getUtfString("package"), controller.dataType)
                ?: throw Exception("Could not find package")
            val blockRewardType = BLOCK_REWARD_TYPE.valueOf(data.getInt("reward_type"))
            val result =
                controller.masterUserManager.userAutoMineManager.buyPackage(packageAutoMine, blockRewardType)

            //Load lại token sau khi mua auto mine
            val blockRewardManager = controller.masterUserManager.blockRewardManager
            blockRewardManager.loadUserBlockReward()

            result.putSFSArray("rewards", blockRewardManager.toSfsArrays())
            sendSuccess(controller, requestId, result)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}