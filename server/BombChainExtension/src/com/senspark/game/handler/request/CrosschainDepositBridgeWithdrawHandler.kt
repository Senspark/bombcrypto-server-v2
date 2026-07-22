package com.senspark.game.handler.request

import com.senspark.game.controller.IUserController
import com.senspark.game.controller.LegacyUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.crosschainDepositBridge.ICrosschainDepositBridgeResponseManager
import com.senspark.game.manager.crosschainDepositBridge.dto.CrosschainDepositBridgeWithdrawOutcome
import com.senspark.game.manager.crosschainDepositBridge.dto.crosschainDepositBridgeChainFromClientOrNull
import com.senspark.game.manager.crosschainDepositBridge.dto.crosschainDepositBridgeChainOf
import com.smartfoxserver.v2.entities.data.ISFSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Phase 9 — user-relayed withdraw (MAX). The client picks the withdraw network (SFSField.CHAIN); the server
 * validates + forwards a withdraw-sign request to ap-deposit-bridge, awaits the EIP-191 signature, and
 * returns it on this request. The user then relays `withdraw(token, otherDeposited, deadline, signature)`
 * themselves. No amount input, no balance debit, no pending — the contract self-computes the amount.
 */
class CrosschainDepositBridgeWithdrawHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.CROSSCHAIN_DEPOSIT_BRIDGE_WITHDRAW

    // WebSocket → the request can respond as late as needed. We run on a coroutine, forward the withdraw-sign
    // request to ap-deposit-bridge, await its result, then respond on this request.
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
                // Withdraw network = the client-chosen chain (the user relays ON this chain), else login chain.
                val withdrawChain = (if (data.containsKey(SFSField.CHAIN))
                    crosschainDepositBridgeChainFromClientOrNull(data.getUtfString(SFSField.CHAIN)) else null)
                    ?: crosschainDepositBridgeChainOf(controller.dataType)

                controller.masterUserManager.crosschainDepositBridgeManager
                    .validateWithdrawRequest(rewardType, withdrawChain)

                val outcome = controller.svServices.get<ICrosschainDepositBridgeResponseManager>()
                    .beginWithdraw(controller, walletAddress, rewardType, withdrawChain)

                when (outcome) {
                    is CrosschainDepositBridgeWithdrawOutcome.Ok ->
                        sendSuccess(controller, requestId, outcome.payload)
                    is CrosschainDepositBridgeWithdrawOutcome.Rejected ->
                        sendError(controller, requestId, outcome.code, outcome.safeMessage)
                }
            } catch (ex: Exception) {
                sendExceptionError(controller, requestId, ex)
            }
        }
    }
}
