package com.senspark.game.extension.schedulers

import com.senspark.common.service.IScheduler
import com.senspark.common.utils.IServerLogger
import com.senspark.game.data.manager.treassureHunt.ICoinRankingManager
import com.senspark.game.manager.ton.IClubManager
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

class ExtensionSchedulerTon(
    private val _scheduler: IScheduler,
    private val _logger: IServerLogger,
    private val _coinRankingManager: ICoinRankingManager,
    private val _clubManager: IClubManager,
) : IExtensionScheduler {

    override fun initialize() {
        initScheduleCoinRanking()
        initScheduleClubSummary()
    }

    private fun initScheduleCoinRanking() {
        val taskName = getTasksName("coin ranking")
        _scheduler.schedule(taskName, 0, 5.minutes.inWholeMilliseconds.toInt()) {
            try {
                _coinRankingManager.reload()
            } catch (e: Exception) {
                _logger.error("$taskName ${e.message}")
            }
        }
    }

    private fun initScheduleClubSummary() {
        val today = LocalDate.now()

        val midnight = today.plusDays(1).atStartOfDay(ZoneOffset.UTC)
        val initialDelay = midnight.toInstant().toEpochMilli() - Instant.now().toEpochMilli()
        val period = TimeUnit.DAYS.toMillis(1)
        val taskName = getTasksName("club summary")

        _scheduler.schedule(taskName, initialDelay.toInt(), period.toInt()) {
            try {
                _clubManager.summaryClubPoint()
                _clubManager.summaryClubBid()
            } catch (e: Exception) {
                _logger.error("$taskName ${e.message}")
            }
        }
    }

    private fun getTasksName(name: String): String {
        return "$name-TON"
    }
}