package com.senspark.game.manager.crosschainDepositBridge

import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.manager.crosschainDepositBridge.dto.CrosschainDepositBridgeChain
import com.smartfoxserver.v2.entities.data.ISFSObject

/**
 * Per-user cross-chain deposit bridge helpers (Phase 9 — user relays their own withdraw). SmartFox only
 * validates the request against the kill-switch + identity and serializes the signer's response; the
 * withdraw amount is self-computed on-chain and the displayed balance is read on-chain, so there is no
 * gross computation, no pending, and no balance mutation here.
 */
interface ICrosschainDepositBridgeManager {
    /**
     * Withdraw guards: kill-switch (bridge_withdraw_enabled), wallet present, supported reward type.
     * Throws [com.senspark.game.exception.CustomException] with a client-safe message on failure.
     * No amount, no balance read, no pending — withdraw is MAX and the contract self-computes it.
     */
    fun validateWithdrawRequest(rewardType: BLOCK_REWARD_TYPE, chain: CrosschainDepositBridgeChain)

    /**
     * Build the withdraw-sign response the client relays to the contract: the EIP-191 signature plus the
     * parameters `withdraw(token, otherDeposited, deadline, signature)` needs, and the target chain.
     */
    fun buildSignatureResult(
        signature: String,
        otherDeposited: String,
        deadline: Long,
        bridgeAddress: String,
        tokenAddress: String,
        chain: CrosschainDepositBridgeChain,
    ): ISFSObject
}
