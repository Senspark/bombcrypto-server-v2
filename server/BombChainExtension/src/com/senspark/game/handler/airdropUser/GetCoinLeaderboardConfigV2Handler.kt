package com.senspark.game.handler.airdropUser

import com.senspark.common.utils.toSFSArray
import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.treassureHunt.ICoinRankingManager
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetCoinLeaderboardConfigV2Handler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_COIN_LEADERBOARD_CONFIG_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val coinRankingManager = controller.svServices.get<ICoinRankingManager>()
            val dataConfigArray = coinRankingManager.configLeaderboard.toSFSArray {
                it.toSFSObject()
            }
            return sendSuccess(
                controller, requestId,
                SFSObject().apply { putSFSArray("data", dataConfigArray) }
            )
        } catch (ex: Exception) {
            return sendExceptionError(controller, requestId, ex)
        }
    }
}