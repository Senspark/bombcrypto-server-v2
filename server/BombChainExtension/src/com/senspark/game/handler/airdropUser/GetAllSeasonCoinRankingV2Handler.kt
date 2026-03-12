package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.treassureHunt.ICoinRankingManager
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetAllSeasonCoinRankingV2Handler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_ALL_SEASON_COIN_RANKING_V2


    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val response = SFSObject()
        try {
            val coinRankingManager = controller.svServices.get<ICoinRankingManager>()
            response.putSFSObject(
                "current_rank",
                coinRankingManager.getCurrentRanking(controller.userId, true, controller.dataType)
            )
            response.putSFSArray("rank_list", coinRankingManager.toSFSArray(true, controller.dataType))
            return sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            return sendExceptionError(controller, requestId, ex)
        }
    }
}