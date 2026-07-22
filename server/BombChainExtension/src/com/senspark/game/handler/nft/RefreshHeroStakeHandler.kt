package com.senspark.game.handler.nft

import com.senspark.game.api.IBlockchainDatabaseManager
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.KickReason
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Client-triggered hero stake refresh.
 *
 * Mirrors the V4 fire-and-forget pattern: ack the client immediately, then
 * POST to ap-blockchain on Dispatchers.IO. ap-blockchain reads on-chain stake
 * and publishes to AP_BL_HERO_STAKE_STR — the existing AllHeroesFiManager
 * consumer applies it and pushes BheroStakePush to the user if online.
 */
class RefreshHeroStakeHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.REFRESH_HERO_STAKE

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.checkHash()) {
            controller.disconnect(KickReason.CHEAT_LOGIN)
            return
        }
        try {
            val heroId = data.getLong(SFSField.ID).toInt()
            val txHash = data.getUtfString(SFSField.TxHash) ?: ""
            if (txHash.isEmpty()) {
                return sendError(controller, requestId, ErrorCode.SERVER_ERROR, "missing tx_hash")
            }

            val uid = controller.userId
            val dataType = controller.dataType
            val databaseManager = controller.svServices.get<IBlockchainDatabaseManager>()

            sendSuccess(controller, requestId, SFSObject())

            coroutine.scope.launch(Dispatchers.IO) {
                val ok = databaseManager.heroDatabase.refreshHeroStake(uid, heroId, dataType, txHash)
                controller.logger.log("[REFRESH_HERO_STAKE] uid=$uid hero=$heroId tx=$txHash ok=$ok")
            }
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}
