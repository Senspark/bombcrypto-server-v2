package com.senspark.game.manager.crosschainDepositBridge

import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.SFSField
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.blockReward.IUserBlockRewardManager
import com.senspark.game.manager.crosschainDepositBridge.dto.CrosschainDepositBridgeChain
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class CrosschainDepositBridgeManager(
    private val _mediator: UserControllerMediator,
    @Suppress("unused") private val blockRewardManager: IUserBlockRewardManager,
) : ICrosschainDepositBridgeManager {

    private val gameConfigManager = _mediator.services.get<IGameConfigManager>()

    override fun validateWithdrawRequest(rewardType: BLOCK_REWARD_TYPE, chain: CrosschainDepositBridgeChain) {
        if (!gameConfigManager.bridgeWithdrawEnabled) {
            throw CustomException("This feature will be coming soon.")
        }
        if (_mediator.walletAddress.isNullOrEmpty()) {
            throw CustomException("You aren't user FI")
        }
        when (rewardType) {
            BLOCK_REWARD_TYPE.BCOIN_BRIDGE, BLOCK_REWARD_TYPE.SEN_BRIDGE -> Unit
            else -> throw CustomException("Invalid token ${rewardType.name}")
        }
    }

    override fun buildSignatureResult(
        signature: String,
        otherDeposited: String,
        deadline: Long,
        bridgeAddress: String,
        tokenAddress: String,
        chain: CrosschainDepositBridgeChain,
    ): ISFSObject {
        val result: ISFSObject = SFSObject()
        result.putInt("code", 0)
        result.putUtfString(SFSField.signature, signature)
        result.putUtfString(SFSField.OTHER_DEPOSITED, otherDeposited)
        result.putLong(SFSField.DEADLINE, deadline)
        result.putUtfString(SFSField.BRIDGE_ADDRESS, bridgeAddress)
        result.putUtfString(SFSField.TOKEN_ADDRESS, tokenAddress)
        result.putUtfString(SFSField.CHAIN, chain.dbName)
        return result
    }
}
