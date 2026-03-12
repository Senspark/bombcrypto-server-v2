package com.senspark.game.handler.pvp

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.pvp.IPvpRankingRewardManager
import com.senspark.game.data.manager.season.IPvpSeasonManager
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetPvpRankingHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_PVP_RANKING_V2

    /**
     * FIXME: Bad optimization
     */
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val response = SFSObject()
        try {
            val pvpRankingRewardManager = controller.svServices.get<IPvpRankingRewardManager>()
            val pvpSeasonManager = controller.svServices.get<IPvpSeasonManager>()
            val minPvpMatchCountToGetReward = services.get<IGameConfigManager>().minPvpMatchCountToGetReward

            response.putInt("remain_time", pvpSeasonManager.currentSeason.getRemainingTime().toInt())
            response.putInt("pvp_current_season", pvpSeasonManager.currentSeasonNumber)
            response.putInt("total_count", controller.countUserRanked())
            response.putSFSObject("current_rank", controller.pvPRanking)
            response.putSFSArray("rank_list", controller.pvPRankingList)
            response.putSFSObject(
                "reward",
                pvpRankingRewardManager.getReward(controller.userId).toSFSObject(minPvpMatchCountToGetReward)
            )
            response.putBool("pvp_season_valid", true)
            response.putSFSArray("pvp_ranking_reward", pvpRankingRewardManager.getConfigPvpRankingReward())
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
        return sendSuccess(controller, requestId, response)
    }
}