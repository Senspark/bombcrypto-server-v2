package com.senspark.game.handler.request

import com.senspark.game.controller.IUserController
import com.senspark.game.controller.LegacyUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.crosschainDepositBridge.ICrosschainDepositBridgeResponseManager
import com.senspark.game.manager.crosschainDepositBridge.dto.CrosschainDepositBridgeNotifyKind
import com.senspark.game.manager.crosschainDepositBridge.dto.crosschainDepositBridgeChainFromClientOrNull
import com.senspark.game.manager.crosschainDepositBridge.dto.crosschainDepositBridgeChainOf
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Phase 9 smart-sweep — the client reports a bridge activity (before/after deposit, after withdraw) so the
 * indexer can accelerate its getLogs sweep right when the on-chain event happens instead of waiting a full
 * idle cycle. Fire-and-forget: we validate identity, re-emit the activity on SV_DEPBRIDGE_NOTIFY_STR with the
 * FULL payload (wallet from the session, never the client), and ack. No balance effect, no signature.
 */
class CrosschainDepositBridgeNotifyHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.CROSSCHAIN_DEPOSIT_BRIDGE_NOTIFY

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        controller as LegacyUserController
        coroutine.scope.launch(Dispatchers.IO) {
            try {
                val walletAddress = controller.walletAddress
                if (controller.userInfo.type != EnumConstants.UserType.FI || walletAddress.isNullOrEmpty()) {
                    throw CustomException("You aren't user FI")
                }
                val kind = data.getUtfString(SFSField.KIND)
                if (kind !in CrosschainDepositBridgeNotifyKind.CLIENT_KINDS) {
                    throw CustomException("Unsupported bridge notify kind")
                }
                // Chain the activity happens ON: client-picked, else login chain.
                val chain = (if (data.containsKey(SFSField.CHAIN))
                    crosschainDepositBridgeChainFromClientOrNull(data.getUtfString(SFSField.CHAIN)) else null)
                    ?: crosschainDepositBridgeChainOf(controller.dataType)
                val rewardType = EnumConstants.BLOCK_REWARD_TYPE.valueOf(data.getInt(SFSField.BLOCK_REWARD_TYPE))
                val txHash = if (data.containsKey(SFSField.TX_HASH)) data.getUtfString(SFSField.TX_HASH) else null

                controller.svServices.get<ICrosschainDepositBridgeResponseManager>()
                    .publishNotify(kind, walletAddress, chain, rewardType, txHash)

                sendSuccess(controller, requestId, SFSObject())
            } catch (ex: Exception) {
                sendExceptionError(controller, requestId, ex)
            }
        }
    }
}
