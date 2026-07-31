package com.senspark.game.handler.request

import com.senspark.game.controller.IUserController
import com.senspark.game.controller.LegacyUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.nativeDeposit.INativeDepositManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * User-relayed native (BNB / POL) withdraw (withdraw-MAX). Returns a signed
 * { signature, allowed_cumulative, deadline, contract, chainId, chain } the user relays on-chain.
 */
class WithdrawNativeHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.WITHDRAW_NATIVE

    // WebSocket → the request can respond as late as needed. Run on IO (blocking HTTP + DB), respond here.
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        controller as LegacyUserController
        coroutine.scope.launch(Dispatchers.IO) {
            try {
                if (controller.disableWhileLoginByAccount()) {
                    return@launch sendError(controller, requestId, ErrorCode.PERMISSION_DENIED, null)
                }
                val walletAddress = controller.walletAddress
                if (controller.userInfo.type != EnumConstants.UserType.FI || walletAddress.isNullOrEmpty()) {
                    throw CustomException("You aren't user FI")
                }
                val rewardType = EnumConstants.BLOCK_REWARD_TYPE.valueOf(data.getInt(SFSField.BLOCK_REWARD_TYPE))
                val payload = controller.svServices.get<INativeDepositManager>()
                    .requestWithdraw(controller.userId, walletAddress, rewardType)
                sendSuccess(controller, requestId, payload)
            } catch (ex: Exception) {
                sendExceptionError(controller, requestId, ex)
            }
        }
    }
}
