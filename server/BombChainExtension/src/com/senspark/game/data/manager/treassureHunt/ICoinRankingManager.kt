package com.senspark.game.data.manager.treassureHunt

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.config.CoinLeaderboardConfig
import com.senspark.game.data.model.config.Season
import com.senspark.game.declare.EnumConstants.DataType
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject

interface ICoinRankingManager : IServerService {
    val currentSeason: Season
    val currentSeasonNumber: Int
    val configLeaderboard: List<CoinLeaderboardConfig>
    fun reload()
    fun saveRankingCoin(uid: Int, coin: Float, network: DataType)
    fun getCurrentRanking(userId: Int, isAllSeason: Boolean, network: DataType): ISFSObject
    fun toSFSArray(isAllSeason: Boolean, network: DataType): ISFSArray
}