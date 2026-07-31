package com.senspark.game.handler.iapshop

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

// The client sends no price: the server recomputes the charge from config_native_rate at purchase time.
class UserBuyGemByNativeTokenHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.BUY_GEM_BY_NATIVE_TOKEN

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val productId = data.getUtfString("product_id")

            controller.masterUserManager.userIAPShopManager.buyByNativeToken(productId)

            val blockRewardManager = controller.masterUserManager.blockRewardManager
            blockRewardManager.loadUserBlockReward()

            val response = SFSObject.newInstance().apply {
                putBool("success", true)
                putSFSArray("rewards", blockRewardManager.toSfsArrays())
            }
            return sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}
