package com.senspark.game.pvp.manager

import com.senspark.common.data.IBombRank
import com.senspark.common.pvp.IRankManager
import com.senspark.common.pvp.IRankResult
import com.senspark.common.utils.toSFSArray
import com.senspark.game.data.manager.season.IPvpSeasonManager
import com.senspark.game.data.model.user.BombRank
import com.senspark.game.pvp.strategy.rank.CasualRankStrategy
import com.senspark.game.pvp.strategy.rank.SeasonRankStrategy
import com.senspark.game.service.IPvpDataAccess
import com.smartfoxserver.v2.entities.data.ISFSArray

class PvpRankManager(
    private val pvpDataAccess: IPvpDataAccess,
    private val _seasonManager: IPvpSeasonManager,
) : IRankManager {
    private val _ranks: MutableList<IBombRank> = mutableListOf()
    private lateinit var _seasonRankStrategy: SeasonRankStrategy
    private val _casualRankStrategy = CasualRankStrategy(this)

    override fun initialize() {
        _ranks.addAll(pvpDataAccess.queryRankingWithPoint())
        _seasonRankStrategy = SeasonRankStrategy(this, _ranks)
    }

    override fun getRank(point: Int): Int {
        return _ranks.first { point in it.startPoint until it.endPoint }.bombRank
    }

    override fun getBombRank(point: Int): IBombRank {
        return _ranks.first { point in it.startPoint until it.endPoint }
    }

    override fun calculate(
        isDraw: Boolean,
        isWinner: Boolean,
        slot: Int,
        boosters: List<Int>,
        points: List<Int>
    ): IRankResult {
        val strategy =
            if (_seasonManager.currentSeason.seasonEnded) _casualRankStrategy
            else _seasonRankStrategy
        return strategy.calculate(
            isDraw,
            isWinner,
            slot,
            boosters,
            points,
        )
    }

    override fun toSFSArray(): ISFSArray {
        return _ranks.toSFSArray { BombRank.toSfsObject(it) }
    }

    override fun getAmountPvpMatchesCurrentDate(uid: Int): Int {
        return pvpDataAccess.getAmountPvpMatchesCurrentDate(uid,_seasonManager.currentSeasonNumber)
    }

    override fun destroy() = Unit
}