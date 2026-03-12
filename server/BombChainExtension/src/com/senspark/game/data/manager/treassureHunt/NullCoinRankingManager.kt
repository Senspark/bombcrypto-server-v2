package com.senspark.game.data.manager.treassureHunt

import com.senspark.game.data.model.config.CoinLeaderboardConfig
import com.senspark.game.data.model.config.Season
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class NullCoinRankingManager : ICoinRankingManager {

    override val currentSeason: Season get() = throw CustomException("Feature not support")
    override val currentSeasonNumber: Int get() = 0
    override val configLeaderboard: List<CoinLeaderboardConfig> get() = emptyList()

    override fun initialize() {
    }

    override fun reload() {}

    override fun getCurrentRanking(userId: Int, isAllSeason: Boolean, network: DataType): ISFSObject {
        return SFSObject()
    }

    override fun saveRankingCoin(uid: Int, coin: Float, network: DataType) {}

    override fun toSFSArray(isAllSeason: Boolean, network: DataType): ISFSArray {
        return SFSArray()
    }
}