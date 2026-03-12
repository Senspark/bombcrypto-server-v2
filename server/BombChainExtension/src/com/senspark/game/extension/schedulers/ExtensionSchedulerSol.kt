package com.senspark.game.extension.schedulers

import com.senspark.common.service.IScheduler
import com.senspark.common.utils.IServerLogger
import com.senspark.game.data.manager.treassureHunt.ICoinRankingManager
import kotlin.time.Duration.Companion.minutes

class ExtensionSchedulerSol(
    private val _scheduler: IScheduler,
    private val _logger: IServerLogger,
    private val _coinRankingManager: ICoinRankingManager,
) : IExtensionScheduler {

    override fun initialize() {
        initScheduleCoinRanking()
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

    private fun getTasksName(name: String): String {
        return "$name-SOL"
    }
}