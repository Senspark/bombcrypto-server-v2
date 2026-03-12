package com.senspark.game.extension.schedulers

import com.senspark.common.service.IScheduler
import com.senspark.common.utils.IGlobalLogger
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.extension.GlobalServices
import com.senspark.lib.data.manager.IGameConfigManager
import kotlin.time.Duration.Companion.minutes

class ExtensionSchedulerAll(
    service: GlobalServices,
) : IExtensionScheduler {

    override fun initialize() {
        scheduleReloadGameConfig()
    }

    private val _scheduler = service.get<IScheduler>()
    private val _logger = service.get<IGlobalLogger>()
    private val _dataAccessManager = service.get<IDataAccessManager>()
    private val _gameConfigManager = service.get<IGameConfigManager>()

    private fun scheduleReloadGameConfig() {
        val taskName = getTasksName("scheduleReloadGameConfig")
        _scheduler.schedule(taskName, 0, 2.minutes.inWholeMilliseconds.toInt()) {
            try {
                val hashGameConfig = _dataAccessManager.libDataAccess.loadGameConfig()
                _gameConfigManager.initialize(hashGameConfig)
            } catch (ex: Exception) {
                _logger.error("$taskName ${ex.message}")
            }
        }
    }

    private fun getTasksName(name: String): String {
        return "$name-ALL"
    }
}