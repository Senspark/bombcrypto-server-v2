package com.senspark.game.extension.schedulers

import com.senspark.common.service.IScheduler
import com.senspark.common.utils.IServerLogger
import com.senspark.game.extension.GlobalServices
import com.senspark.game.extension.ServerServices
import com.senspark.game.pvp.service.PvpFeeProcessor
import com.senspark.game.service.DatabaseStatement
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.hours

/**
 * ExtensionSchedulerPvp manages scheduled tasks specific to the PVP system,
 * such as fee processing and potentially ranking resets.
 */
class ExtensionSchedulerPvp(
    service: GlobalServices,
    netService: ServerServices,
    private val _dbStatement: DatabaseStatement
) : IExtensionScheduler {

    private val _scheduler = service.get<IScheduler>()
    private val _logger = netService.get<IServerLogger>()
    private val _feeProcessor = PvpFeeProcessor(
        _dbStatement.subUserReward.let { 
            // We need the raw IDatabase, which is usually in the GlobalServices as IDatabase
            service.get<com.senspark.common.IDatabase>() 
        },
        _logger,
        _dbStatement
    )

    override fun initialize() {
        scheduleFeeProcessing()
    }

    /**
     * Schedules the fee processor to run every hour.
     */
    private fun scheduleFeeProcessing() {
        val taskName = "PvpFeeBatchProcessing"
        val period = 1.hours.inWholeMilliseconds.toInt()
        
        _scheduler.schedule(taskName, 0, period) {
            try {
                _feeProcessor.processPendingFees()
            } catch (e: Exception) {
                _logger.error("$taskName Error: ${e.message}")
            }
        }
    }
}
