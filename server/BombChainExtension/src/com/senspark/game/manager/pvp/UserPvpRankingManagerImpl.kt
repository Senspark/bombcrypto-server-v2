package com.senspark.game.manager.pvp

import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.manager.pvp.IPvpRankingRewardManager
import com.senspark.game.data.manager.season.IPvpSeasonManager
import com.senspark.game.data.model.PvpReward
import com.senspark.game.data.model.config.UserPvpRankingReward
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.customEnum.ChangeRewardReason
import com.senspark.game.exception.CustomException
import com.senspark.game.utils.deserialize
import com.senspark.lib.data.manager.GameConfigManager
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.SFSObject

class UserPvpRankingManagerImpl(
    private val _mediator: UserControllerMediator,
) : IUserPvpRankingManager {

    private val dataAccessManager = _mediator.services.get<IDataAccessManager>()
    private val configManager = _mediator.services.get<IGameConfigManager>()
    private val pvpSeasonManager = _mediator.svServices.get<IPvpSeasonManager>()
    private val pvpRankingRewardManager = _mediator.svServices.get<IPvpRankingRewardManager>()
        
    override fun claimReward(): SFSObject {
        val results = SFSObject()
        val rankReward = SFSObject()
        val currentRankReward = pvpRankingRewardManager.getReward(_mediator.userId)
        rankReward.putInt("rank_number", currentRankReward.rank)
        rankReward.putInt("is_claim", currentRankReward.isClaim)
        rankReward.putInt("total_match", currentRankReward.totalMatch)
        rankReward.putInt("pvp_match_reward", configManager.minPvpMatchCountToGetReward)
        rankReward.putSFSObject(
            "reward",
            SFSObject.newFromJsonData(currentRankReward.reward ?: SFSObject.newInstance().toJson())
        )
        if (updateUserPvpClaimReward(currentRankReward)) {
            rankReward.putInt("is_claim", 1)
        }
        results.putSFSObject("rank_reward", rankReward)
        return results
    }

    private fun updateUserPvpClaimReward(rs: UserPvpRankingReward): Boolean {
        val reward = rs.reward
        val totalMatch = rs.totalMatch
        val hasClaimReward =
            dataAccessManager.userDataAccess.hasClaimReward(_mediator.userId, pvpSeasonManager.currentRewardSeasonNumber)
        if (rs.isClaim == 0 && hasClaimReward) {
            rs.isClaim = 1
            //update is_claim
            val wrapperClaim = dataAccessManager.gameDataAccess.updateUserRankRewardClaim(
                _mediator.userId, pvpSeasonManager.currentRewardSeasonNumber, 1
            )
            var mapReward: Map<EnumConstants.BLOCK_REWARD_TYPE, Float>? = null
            if (reward != null && totalMatch >= configManager.minPvpMatchCountToGetReward) {
                //update reward
                val rewards = deserialize<PvpReward>(reward)
                mapReward = rewards.parseReward()
            }
            dataAccessManager.gameDataAccess.updateUserReward(
                _mediator.userId,
                _mediator.dataType,
                mapReward,
                listOf(wrapperClaim),
                ChangeRewardReason.CLAIM_PVP_RANKING_REWARD
            )
            return true
        } else {
            rs.isClaim = 1
            throw CustomException("You have received this reward", ErrorCode.NOT_CLAIM_PVP_REWARD)
        }
    }
}