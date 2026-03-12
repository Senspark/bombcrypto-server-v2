package com.senspark.game.extension.schedulers

import com.senspark.common.service.IScheduler
import com.senspark.common.utils.IServerLogger
import com.senspark.common.utils.toSFSArray
import com.senspark.game.api.IServerInfoManager
import com.senspark.game.controller.LegacyUserController
import com.senspark.game.data.manager.pvp.IPvpRankingManager
import com.senspark.game.data.manager.pvp.IPvpRankingRewardManager
import com.senspark.game.data.manager.season.IPvpSeasonManager
import com.senspark.game.data.manager.treassureHunt.ICoinRankingManager
import com.senspark.game.data.model.config.UserPvpRankingReward
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.GameConstants.SCHEDULE_STATUS
import com.senspark.game.declare.SFSCommand.TH_MODE_V2_REWARDS
import com.senspark.game.extension.GlobalServices
import com.senspark.game.extension.ServerServices
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IUsersManager
import com.senspark.game.manager.convertToken.ISwapTokenRealtimeManager
import com.senspark.game.manager.dailyTask.IDailyTaskManager
import com.senspark.game.manager.market.IMarketManager
import com.senspark.game.manager.treasureHuntV2.ITreasureHuntV2Manager
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

class ExtensionSchedulerRon(
    service: GlobalServices,
    netService: ServerServices,
) : IExtensionScheduler {

    private val _scheduler = service.get<IScheduler>()
    private val _logger = netService.get<IServerLogger>()
    private val _coinRankingManager = netService.get<ICoinRankingManager>()

    private val _configManager = service.get<IGameConfigManager>()
    private val _envManager = service.get<IEnvManager>()
    private val _userDataAccess = service.get<IUserDataAccess>()
    private val _usersManager = netService.get<IUsersManager>()
    private val _pvpRankingManager = netService.get<IPvpRankingManager>()
    private val _pvpSeasonManager = netService.get<IPvpSeasonManager>()
    private val _pvpRankingRewardManager = netService.get<IPvpRankingRewardManager>()
    private val _thManager = netService.get<ITreasureHuntV2Manager>()
    private val _swapTokenManager = netService.get<ISwapTokenRealtimeManager>()
    private val _serverInfoManager = netService.get<IServerInfoManager>()
    private val _dailyTaskManager = netService.get<IDailyTaskManager>()
    private val _marketManager = netService.get<IMarketManager>()

    override fun initialize() {
        initScheduleCoinRanking()

        scheduleGetServerInfo()
        scheduleSwapTokenPrice()
        scheduleCoinRanking()
        scheduleRefillThModePool()
        scheduleTHModeReward()
        scheduleAutoDecayPointPvp()
        schedulePvpSummary()
        schedulePvpRanking()
        scheduleChangeDailyTask()
        scheduleRefreshMinPriceMarket()
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

    /**
     * Cập nhật lại số lượng user đang online
     */
    private fun scheduleGetServerInfo() {
        if (!_serverInfoManager.isEnable()) {
            return
        }
        val taskName = getTasksName("scheduleGetServerInfo")

        _scheduler.schedule(taskName, 0, _serverInfoManager.getServerInfoTimeUpdate() * 1000) {
            try {
                _serverInfoManager.reloadUserOnline()
            } catch (e: Exception) {
                _logger.error("$taskName ${e.message}")
            }
        }
    }

    private fun scheduleSwapTokenPrice() {
        val taskName = getTasksName("scheduleSwapTokenPrice")
        _scheduler.schedule(
            taskName,
            0,
            _swapTokenManager.getTimeUpdatePrice() * 60 * 1000
        ) {
            try {
                _swapTokenManager.reloadTokenPrice()
            } catch (e: Exception) {
                _logger.error("$taskName ${e.message}")
            }
        }
    }

    private fun scheduleCoinRanking() {
        val taskName = getTasksName("scheduleCoinRanking")
        _scheduler.schedule(taskName, 0, 5.minutes.inWholeMilliseconds.toInt()) {
            try {
                _coinRankingManager.reload()
            } catch (e: Exception) {
                _logger.error("$taskName ${e.message}")
            }
        }
    }

    private fun scheduleRefillThModePool() {
        val taskName = getTasksName("scheduleRefillThModePool")
        // Refill sau 10000 millisecond để tránh xung đột khi phát thưởng th mode v2
        val today = LocalDate.now()

        val midnight = today.plusDays(1).atStartOfDay(ZoneOffset.UTC)
        val initialDelay = midnight.toInstant().toEpochMilli() - Instant.now().toEpochMilli() + 10000
        val period = TimeUnit.DAYS.toMillis(1)



        _scheduler.schedule(taskName, initialDelay.toInt(), period.toInt()) {
            try {
                _thManager.refillRewardPool()
                _swapTokenManager.refillRemainingTotalSwap()
            } catch (e: Exception) {
                _logger.error("$taskName ${e.message}")
            }
        }
    }

    private fun scheduleTHModeReward() {
        val period = _thManager.period
        val taskName = getTasksName("scheduleTHModeReward")
        _scheduler.schedule(taskName, 0, period * 1000) {
            try {
                val listUserRewards = _thManager.calculateReward()
                for ((userId, userReward) in listUserRewards) {
                    val sumRewardsByType = _thManager.sumReward(userReward)
                    val rewardDetailByType = _thManager.getRewardDetail(userReward)
                    val data = SFSObject()
                    val array = SFSArray()
                    for ((type, reward) in sumRewardsByType) {
                        val sfsObject = SFSObject()
                        sfsObject.putInt("block_type", type.value)
                        sfsObject.putFloat("reward", reward.value)
                        sfsObject.putSFSArray("reward_detail", rewardDetailByType[type]?.toSFSArray {
                            SFSObject().apply {
                                putInt("pool_id", it.key)
                                putFloatArray("list_reward", it.value.map { it.value })
                            }
                        } ?: SFSArray())
                        array.addSFSObject(sfsObject)
                    }
                    data.putSFSArray("data", array)
                    _usersManager.getUserController(userId.userId)?.let {
                        it as LegacyUserController
                        it.masterUserManager.blockRewardManager.addRewards(sumRewardsByType)
                        it.setNeedSave(EnumConstants.SAVE.REWARD)
                        it.sendDataEncryption(TH_MODE_V2_REWARDS, data)
                    }
                }
                _thManager.saveRewardPool()
            } catch (e: Exception) {
                _logger.error("$taskName ${e.message}")
            }
        }
    }

    fun scheduleAutoDecayPointPvp() {
        val taskName = getTasksName("scheduleAutoDecayPointPvp")
        val today = LocalDate.now()

        val midnight = today.plusDays(1).atStartOfDay(ZoneOffset.UTC)
        val initialDelay = midnight.toInstant().toEpochMilli() - Instant.now().toEpochMilli()
        val period = TimeUnit.DAYS.toMillis(1)

        _scheduler.schedule(taskName, initialDelay.toInt(), period.toInt()) {
            try {
                // auto trừ rank user nếu không đạt điều kiện mỗi ngày
                _pvpRankingManager.decayUserRank()
            } catch (e: Exception) {
                _logger.error("$taskName ${e.message}")
            }
        }
    }

    /**
     * Cập nhật lại thứ tự Ranking PVP
     */
    private fun schedulePvpSummary() {
        val taskName = getTasksName("schedulePvpSummary")
        _scheduler.schedule(taskName, 0, _envManager.pvpRankUpdateSeconds * 1000) {
            try {
                _userDataAccess.summaryPvpRankingReward()
            } catch (e: Exception) {
                _logger.error("$taskName ${e.message}")
            }
        }
    }

    private fun schedulePvpRanking() {
        val taskName = getTasksName("schedulePvpRanking")
        _scheduler.schedule(taskName, 0, 10.minutes.inWholeMilliseconds.toInt()) {
            try {
                // update pvp ranking
                _pvpRankingManager.reload()
                _pvpSeasonManager.currentSeasonNumber.let {
                    val isLoadDataManager = _userDataAccess.isLoadDataManager(
                        SCHEDULE_STATUS.PVP, it
                    )
                    if (isLoadDataManager) {
                        _pvpRankingRewardManager.reload()
                        val rewardMap: Map<Int, UserPvpRankingReward> =
                            if (_pvpSeasonManager.currentRewardSeasonNumber > 1) {
                                _userDataAccess.getPvpRankingReward(
                                    _configManager.minPvpMatchCountToGetReward,
                                    _pvpSeasonManager.currentRewardSeasonNumber
                                )
                            } else {
                                emptyMap()
                            }
                        _pvpRankingRewardManager.setData(rewardMap)
                    }
                }
            } catch (e: Exception) {
                _logger.error("$taskName ${e.message}")
            }
        }
    }

    // Làm mới 5 task cho ngày mới
    private fun scheduleChangeDailyTask() {
        val taskName = getTasksName("scheduleChangeDailyTask")
        val today = LocalDate.now()

        val midnight = today.plusDays(1).atStartOfDay(ZoneOffset.UTC)
        val initialDelay = midnight.toInstant().toEpochMilli() - Instant.now().toEpochMilli()
        val period = TimeUnit.DAYS.toMillis(1)

        _scheduler.schedule(taskName, initialDelay.toInt(), period.toInt()) {
            try {
                _dailyTaskManager.checkCacheAndChangeTask()
            } catch (e: Exception) {
                _logger.error("$taskName ${e.message}")
            }
        }
    }

    private fun scheduleRefreshMinPriceMarket() {
        val taskName = getTasksName("scheduleRefreshMinPriceMarket")
        _scheduler.schedule(taskName, 0, _configManager.refreshMinPriceMarket * 1000) {
            try {
                _marketManager.refreshMinPrice()
            } catch (e: Exception) {
                _logger.error("$taskName ${e.message}")
            }
        }
    }

    private fun getTasksName(name: String): String {
        return "$name-RON"
    }
}