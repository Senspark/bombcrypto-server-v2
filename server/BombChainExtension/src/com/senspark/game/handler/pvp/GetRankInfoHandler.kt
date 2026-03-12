package com.senspark.game.handler.pvp

import com.senspark.common.pvp.IRankManager
import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.pvp.IPvpRankingManager
import com.senspark.game.data.model.user.BombRank
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetRankInfoHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_RANK_INFO_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val response = SFSObject()
        try {
            val pvpRankManager = controller.svServices.get<IRankManager>()
            val pvpRankingManager = controller.svServices.get<IPvpRankingManager>()

            // truyền cả các trường cmd GET_GAME_DATA để sau này bỏ luôn cmd GET_GAME_DATA

            // Do có nhiều client chạy cùng lúc nên mỗi khi lấy điểm pvp phải sync lại với db trước
            controller.reloadPvpRanking()
            val currentPoint = controller.pvpRank.point.value
            val rank = pvpRankManager.getBombRank(currentPoint) as BombRank
            val pointDecayUser = pvpRankingManager.getDecayPointUser(controller.userId)
            response.apply {
                putInt("bomb_rank", rank.bombRank)
                putInt("current_point", currentPoint)
                putInt("decay_point_config", rank.decayPoint)
                putInt("min_matches_config", rank.minMatches)
                putInt("amount_matches_current_date", pvpRankManager.getAmountPvpMatchesCurrentDate(controller.userId))
                putInt("decay_point_user", pointDecayUser)
            }
            return sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            return sendError(controller, requestId, 100, ex.message)
        }
    }
}