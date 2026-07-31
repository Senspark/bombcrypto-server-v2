package com.senspark.game.handler.request

import com.senspark.game.controller.IUserController
import com.senspark.game.controller.LegacyUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.nativeDeposit.INativeDepositManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Client hint to sync native deposits fast. Purely an optimization — the server also syncs on login
 * and via the background reconciler, so correctness never depends on this arriving.
 */
class SyncNativeDepositHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.SYNC_NATIVE_DEPOSIT

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        controller as LegacyUserController
        coroutine.scope.launch(Dispatchers.IO) {
            try {
                val walletAddress = controller.walletAddress
                if (controller.userInfo.type != EnumConstants.UserType.FI || walletAddress.isNullOrEmpty()) {
                    throw CustomException("You aren't user FI")
                }
                val rewardType = EnumConstants.BLOCK_REWARD_TYPE.valueOf(data.getInt(SFSField.BLOCK_REWARD_TYPE))
                controller.svServices.get<INativeDepositManager>()
                    .sync(controller.userId, walletAddress, rewardType)
                val result: ISFSObject = SFSObject()
                result.putInt("code", 0)
                sendSuccess(controller, requestId, result)
            } catch (ex: Exception) {
                sendExceptionError(controller, requestId, ex)
            }
        }
    }
}
