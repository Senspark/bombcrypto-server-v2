package com.senspark.game.data.manager.season

import com.senspark.game.data.model.config.Season
import com.senspark.game.db.IShopDataAccess
import com.senspark.lib.data.manager.IGameConfigManager
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class PvpSeasonManager(
    private val _shopDataAccess: IShopDataAccess,
    private val _gameConfigManager: IGameConfigManager
) : IPvpSeasonManager {
    private val _data: MutableMap<Int, Season> = mutableMapOf()
    private lateinit var _currentSeason: Season

    override val currentSeason: Season
        get() {
            if (_currentSeason.seasonClosed) {
                _currentSeason = _data[_currentSeason.id + 1] ?: addNewSeason()
            }
            return _currentSeason
        }

    override val currentSeasonNumber: Int
        get() {
            return currentSeason.id
        }

    override val currentRewardSeasonNumber: Int
        get() {
            // nếu mùa đã kết thúc nhưng chưa bắt đầu mùa mới thì nhận thuong của mùa hiện tại
            // ngược lại (mùa mới đã bắt đầu thì nhận thưởng mùa trước
            return if (currentSeason.seasonEnded) {
                currentSeason.id
            } else {
                currentSeason.id - 1
            }
        }

    override fun initialize() {
        _data.putAll(_shopDataAccess.loadRankingSeason())
        val now = Instant.now().toEpochMilli()
        _currentSeason = _data.values.firstOrNull {
            (now in it.startDate..it.endDate) ||
                    (it.nextSeasonStartDate != null && now in it.endDate until it.nextSeasonStartDate)
        } ?: addNewSeason()
    }

    private fun getNewSeasonStartTime(): Instant {
        val now = LocalDateTime.now()
        val nextMonth = now.withDayOfMonth(1)
        val midnightUTC = nextMonth.atZone(ZoneId.systemDefault()).withHour(0).withMinute(0).withSecond(0).withNano(0)
        return midnightUTC.toInstant()
    }

    // Obsolete, use getNextSeasonEndTime instead
    private fun getNewSeasonEndTime(): Instant {
        val now = LocalDateTime.now()
        val seasonDay = _gameConfigManager.pvpRankingSeasonDay
        val nextMonth = now.withDayOfMonth(seasonDay)
        val midnightUTC = nextMonth.atZone(ZoneId.systemDefault()).withHour(0).withMinute(0).withSecond(0).withNano(0)
        return midnightUTC.toInstant()
    }

    private fun getNextSeasonEndTime(startTime: Instant): Instant {
        val seasonDay = _gameConfigManager.pvpRankingSeasonDay
        val endTime = LocalDateTime.ofInstant(startTime, ZoneOffset.UTC).plusMonths(1)
            .withDayOfMonth(seasonDay)
            .withHour(0).withMinute(0).withSecond(0).withNano(0)
        return endTime.toInstant(ZoneOffset.UTC)
    }

    private fun getNextNewSeasonStartTime(): Instant {
        val now = LocalDateTime.now()
        val nextMonth = now.plusMonths(1).withDayOfMonth(1)
        val midnightUTC = nextMonth.atZone(ZoneId.systemDefault()).withHour(0).withMinute(0).withSecond(0).withNano(0)
        return midnightUTC.toInstant()
    }

    private fun addNewSeason(): Season {
        val startDateNewSeason = getNewSeasonStartTime()
        val endDateNewSeason = getNextSeasonEndTime(startDateNewSeason)
        val idNewSeason = if (!this::_currentSeason.isInitialized) _data.keys.max() + 1 else _currentSeason.id
        _shopDataAccess.addNewPVPRankingSeason(idNewSeason, startDateNewSeason, endDateNewSeason)
        val newSeason = Season(
            idNewSeason,
            startDateNewSeason.toEpochMilli(),
            endDateNewSeason.toEpochMilli(),
            getNextNewSeasonStartTime().toEpochMilli()
        )
        _data[idNewSeason] = newSeason
        return newSeason
    }

    override fun destroy() = Unit
}