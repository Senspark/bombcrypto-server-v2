package com.senspark.game.data.manager.pvp

import com.senspark.common.utils.toSFSArray
import com.senspark.game.data.model.PvpRankingReward
import com.senspark.game.data.model.config.UserPvpRankingReward
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.utils.serialize
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class PvpRankingRewardManager(
    private val _userDataAccess: IUserDataAccess,
    private val _requiredClaimRewardPlayCount: Int
) : IPvpRankingRewardManager {
    private lateinit var _configPvpRankingReward: List<PvpRankingReward>
    private lateinit var _data: MutableMap<Int, UserPvpRankingReward>
    private val locker = Any()

    override fun initialize() {
    }

    override fun setData(data: Map<Int, UserPvpRankingReward>) {
        synchronized(locker) {
            _data = data.toMutableMap()
        }
    }

    override fun getConfigPvpRankingReward(): ISFSArray {
        return _configPvpRankingReward.toSFSArray { SFSObject.newFromJsonData(it.serialize()) }
    }

    override fun reload() {
        _configPvpRankingReward = _userDataAccess.getConfigPvpRankingReward()
    }

    override fun getReward(userId: Int): UserPvpRankingReward {
        var data = _data[userId]
        if (data == null) {
            data = UserPvpRankingReward(
                userId,
                0,
                0,
                0,
                null,
                _requiredClaimRewardPlayCount
            )
            _data[userId] = data
        }
        return data
    }
}