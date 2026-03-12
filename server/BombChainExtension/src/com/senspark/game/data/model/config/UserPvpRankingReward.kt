package com.senspark.game.data.model.config

import com.senspark.lib.data.manager.GameConfigManager
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.ResultSet

class UserPvpRankingReward(
    val uid: Int,
    val rank: Int,
    var isClaim: Int,
    val totalMatch: Int,
    val reward: String?,
    private val _requiredClaimRewardPlayCount: Int
) {
    companion object {
        fun fromResultSet(rs: ResultSet, pvpMatchReward: Int): UserPvpRankingReward {
            return UserPvpRankingReward(
                rs.getInt("uid"),
                rs.getInt("rank_number"),
                rs.getInt("is_claim"),
                rs.getInt("total_match"),
                rs.getString("reward") ?: null,
                pvpMatchReward
            )
        }
    }

    private fun getRewardDefault(): SFSObject {
        val results = SFSObject()
        val current = SFSObject()
        current.putInt("rank_number", rank)
        current.putInt("is_claim", isClaim)
        current.putSFSObject("reward", SFSObject())
        current.putInt("total_match", totalMatch)
        current.putInt("pvp_match_reward", _requiredClaimRewardPlayCount)
        results.putSFSObject("rank_reward", current)
        return results
    }

    fun toSFSObject(minPvpMatchCountToGetReward: Int): SFSObject {
        if (reward == null || totalMatch < _requiredClaimRewardPlayCount) return getRewardDefault()
        val results = SFSObject()
        val current = SFSObject()
        current.putInt("rank_number", rank)
        current.putInt("is_claim", isClaim)
        current.putSFSObject("reward", SFSObject.newFromJsonData(reward))
        current.putInt("total_match", totalMatch)
        current.putInt("pvp_match_reward", minPvpMatchCountToGetReward)
        results.putSFSObject("rank_reward", current)
        return results
    }

    override fun toString(): String {
        return "uid $uid, isClaim $isClaim, reward $reward"
    }
}