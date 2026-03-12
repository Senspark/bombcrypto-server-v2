package com.senspark.game.data.manager.treassureHunt

import com.senspark.game.data.model.config.CoinLeaderboardConfig
import com.senspark.game.data.model.config.Season
import com.senspark.game.data.model.user.CoinRank
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.db.ITHModeDataAccess
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class CoinRankingManager(
    private val _thModeDataAccess: ITHModeDataAccess,
    private val _shopDataAccess: IShopDataAccess,
    private val _gameConfigManager: IGameConfigManager,
    private val _dataType: DataType,
) : ICoinRankingManager {

    private val _configSeason: MutableMap<Int, Season> = mutableMapOf()
    private val _configLeaderboard: MutableList<CoinLeaderboardConfig> = mutableListOf()
    private lateinit var _currentSeason: Season
    private lateinit var _rankList: List<CoinRank>

    override fun initialize() {
        _configSeason.putAll(_shopDataAccess.loadCoinRankingSeason())
        _configLeaderboard.addAll(_thModeDataAccess.getCoinLeaderboardConfig())

        val now = Instant.now().toEpochMilli()
        _currentSeason = _configSeason.values.firstOrNull {
            (now in it.startDate..it.endDate) ||
                (it.nextSeasonStartDate != null && now in it.endDate until it.nextSeasonStartDate)
        } ?: addNewSeason()
        checkToSetupForNewSeason()
    }

    override val currentSeason: Season
        get() {
            if (_currentSeason.seasonClosed) {
                _currentSeason = _configSeason[_currentSeason.id + 1] ?: addNewSeason()
                checkToSetupForNewSeason()
            }
            return _currentSeason
        }

    override val currentSeasonNumber: Int
        get() {
            return currentSeason.id
        }

    override val configLeaderboard: List<CoinLeaderboardConfig>
        get() {
            return _configLeaderboard
        }

    override fun reload() {
        _rankList = _thModeDataAccess.getRankingCoin(currentSeasonNumber, _dataType)
    }

    override fun getCurrentRanking(userId: Int, isAllSeason: Boolean, network: DataType): ISFSObject {
        val rankList = getRankingList(isAllSeason, network)
        val currentRank = rankList.indexOfFirst { it.uid == userId }
        if (currentRank == -1) return SFSObject()
        val result = rankList[currentRank].toSFSObject(isAllSeason)
        result.putInt("rank", currentRank + 1)
        return result
    }

    override fun saveRankingCoin(uid: Int, coin: Float, network: DataType) {
        if (!currentSeason.seasonEnded) {
            _thModeDataAccess.saveRankingCoin(uid, coin, network, currentSeasonNumber)
        }
    }

    override fun toSFSArray(isAllSeason: Boolean, network: DataType): ISFSArray {
        val result = SFSArray()
        val data: List<CoinRank> = if (isAllSeason) {
            getRankingList(true, network).take(_gameConfigManager.sizeRankingLeaderboard)
        } else {
            getRankingList(false, network)
        }

        for ((index, rank) in data.withIndex()) {
            val sfsObject = rank.toSFSObject(isAllSeason)
            sfsObject.putInt("rank", index + 1)
            result.addSFSObject(sfsObject)
        }
        return result
    }

    private fun getRankingList(isAllSeason: Boolean, network: DataType): List<CoinRank> {
        if (isAllSeason) {
            return _rankList.filter { it.network == network }.sortedByDescending { it.coinTotal }
        }
        return _rankList.filter { it.network == network && it.coinCurrentSeason > 0.0 }
            .sortedByDescending { it.coinCurrentSeason }
    }

    // Gọi hàm này để đảm bảo luôn có sẵn 2 season tiếp theo
    private fun checkToSetupForNewSeason() {
        val existingSeasonIds = _configSeason.keys
        val baseId = if (!this::_currentSeason.isInitialized) existingSeasonIds.maxOrNull() ?: 0 else _currentSeason.id // here return 10, not 11
        
        // Kiểm tra xem có bao nhiêu season tiếp theo kể từ season hiện tại
        val nextSeasons = _configSeason.values
            .filter { it.id > baseId }
            .sortedBy { it.id }
        
        // Chỉ cần đủ 2 season tiếp theo
        if (nextSeasons.size >= 2) {
            return
        }
        
        val seasonsToCreate = 2 - nextSeasons.size
        
        // Start from the last existing next season or current season
        var lastSeasonId = if (nextSeasons.isNotEmpty()) nextSeasons.last().id else baseId
        var lastSeasonEndTime = if (nextSeasons.isNotEmpty()) {
            Instant.ofEpochMilli(nextSeasons.last().endDate)
        } else if (this::_currentSeason.isInitialized) {
            Instant.ofEpochMilli(_currentSeason.endDate)
        } else {
            getNextSeasonStartTime()
        }
        
        // Create needed seasons
        for (i in 1..seasonsToCreate) {
            val newSeasonId = lastSeasonId + 1
            val startTime = lastSeasonEndTime
            val endTime = getNextSeasonEndTime(startTime)
            
            // Add to database
            _thModeDataAccess.addNewAirdropSeason(newSeasonId, startTime, endTime)
            
            // Add to cache
            val seasonStartMs = startTime.toEpochMilli()
            val seasonEndMs = endTime.toEpochMilli()
            val nextSeasonStartMs = seasonEndMs // Most seasons point to the next season start time
            
            val newSeason = Season(
                newSeasonId,
                seasonStartMs,
                seasonEndMs,
                nextSeasonStartMs
            )
            
            _configSeason[newSeasonId] = newSeason
            
            // Update for next iteration
            lastSeasonId = newSeasonId
            lastSeasonEndTime = endTime
        }
    }

    private fun addNewSeason(): Season {
        val endDateNewSeason = getNextSeasonStartTime().toEpochMilli()
        val idNewSeason = if (!this::_currentSeason.isInitialized) _configSeason.keys.max() + 1 else _currentSeason.id
        val startTime = getNextSeasonStartTime()
        _thModeDataAccess.addNewAirdropSeason(idNewSeason, startTime, getNextSeasonEndTime(startTime))
        val newSeason = Season(idNewSeason, _currentSeason.endDate, endDateNewSeason, endDateNewSeason)
        _configSeason[idNewSeason] = newSeason
        return newSeason
    }
    
    private fun getNextSeasonStartTime(): Instant {
        val now = LocalDateTime.now()
        val nextMonth = now.plusMonths(1).withDayOfMonth(_gameConfigManager.coinRankingSeasonDay)
        val midnightUTC = nextMonth.atZone(ZoneOffset.UTC).withHour(0).withMinute(0).withSecond(0).withNano(0)
        return midnightUTC.toInstant()
    }
    
    private fun getNextSeasonEndTime(startTime: Instant): Instant {
        val endTime = LocalDateTime.ofInstant(startTime, ZoneOffset.UTC).plusMonths(1)
            .withDayOfMonth(_gameConfigManager.coinRankingSeasonDay)
            .withHour(0).withMinute(0).withSecond(0).withNano(0)
        return endTime.toInstant(ZoneOffset.UTC)
    }

    private fun createNewSeason(seasonId: Int): Season {
        val startTime = getNextSeasonStartTime()
        val endTime = getNextSeasonEndTime(startTime)
        _thModeDataAccess.addNewAirdropSeason(seasonId, startTime, endTime)
        
        val seasonStartMs = startTime.toEpochMilli()
        val seasonEndMs = endTime.toEpochMilli()
        
        val newSeason = Season(
            seasonId, 
            if (this::_currentSeason.isInitialized) _currentSeason.endDate else seasonStartMs, 
            seasonEndMs, 
            seasonEndMs
        )
        
        _configSeason[seasonId] = newSeason
        return newSeason
    }
}