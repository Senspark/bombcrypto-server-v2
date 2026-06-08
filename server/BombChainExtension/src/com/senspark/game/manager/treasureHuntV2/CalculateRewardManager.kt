package com.senspark.game.manager.treasureHuntV2

import com.senspark.common.utils.ILogger
import com.senspark.game.data.model.config.RewardLevelConfig
import com.senspark.game.data.model.config.TreasureHuntV2Config
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.user.RewardDetail
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.manager.stake.IHeroStakeManager
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

class CalculateRewardManager(
    var treasureHuntV2Config: TreasureHuntV2Config,
    var rewardType: BLOCK_REWARD_TYPE,
    private var _thModeRaceBroadcaster: THModeRaceBroadcaster,
    private val _heroStakeManager: IHeroStakeManager,
    private var _rewardLevelConfig: Map<Int, RewardLevelConfig>,
    private val _logger: ILogger,
) : ICalculateRewardManager {

    companion object {
        private const val POOL_SIZE = 10
        private val POOL_NAMES = arrayOf(
            "Common", "Rare", "Super Rare", "Epic", "Legend", "Super Legend", 
            "Mega", "Super Mega", "Mystic", "Super Mystic"
        )
    }

    /**
     * Công thức:
     * https://docs.google.com/spreadsheets/d/1d_5KfVtbsOQpFV1Ql5vPTceRCG2KmDJ3l0EQNc1rY7I/edit#gid=1813945989
     */

    /**
     * [Rarity, [TicketId, UserHero]]
     */
    private val _pools: Array<ConcurrentHashMap<String, UserHero>> = Array(POOL_SIZE) { ConcurrentHashMap() }
    private val _log = false
    private var _maxUsersForEachPool = _rewardLevelConfig.values.sumOf { it.numUsers }
    private val _minStakeHeroConfig = _heroStakeManager.minStakeHeroConfig

    init {
        for (i in 1 until POOL_SIZE) {
            _pools[i - 1] = ConcurrentHashMap(mapOf())
        }
    }

    override fun setConfig(config: TreasureHuntV2Config, rewardLevelConfig: Map<Int, RewardLevelConfig>) {
        treasureHuntV2Config = config
        _rewardLevelConfig = rewardLevelConfig
        _maxUsersForEachPool = _rewardLevelConfig.values.sumOf { it.numUsers }
    }

    override fun addHeroToPool(hero: Hero, userId: UserId, raceId: Int) {
        val poolIndex = hero.rarity
        if (getRemainingReward(poolIndex) <= 0) {
            consoleLog("Pool ${POOL_NAMES[poolIndex]} is empty")
            return
        }
        val pool = _pools[poolIndex]
        val ticketId = getTicketId(userId.dataType, hero)
        if (!pool.containsKey(ticketId)) {
            pool[ticketId] = UserHero(userId, hero)
        } else {
            pool[ticketId]!!.ticketCount++
        }
        _thModeRaceBroadcaster.sendUserToRedis(
            raceId,
            userId.userId,
            userId.userName,
            hero,
            pool[ticketId]!!.ticketCount,
            poolIndex
        )
    }

    override fun calculateReward(): Map<UserId, List<RewardResult>> {
        val result: MutableMap<UserId, List<RewardResult>> = mutableMapOf()

        for (poolIndex in _pools.indices) {
            val originalPool = _pools[poolIndex]
            if (originalPool.size == 0) {
                continue
            }

            // Lượng reward có thể chia mỗi pool (ví dụ 500000, trừ dần đến khi hết thì thôi)
            var rewardRemaining = getRemainingReward(poolIndex)
            consoleLog("------Pool ${POOL_NAMES[poolIndex]} start: ${rewardRemaining}-------")
            if (rewardRemaining <= 0) {
                continue
            }

            // Clone để tránh user tiếp tục chèn vào pool & tránh thay đổi ticket count
            var clonedPools = originalPool.values.map { UserHero(it.userId, it.hero, it.ticketCount) }
            originalPool.clear()

            clonedPools = clonedPools.sortedByDescending { getScore(it) }

            // Chỉ lấy tối đa _maxUsersForEachPool user
            if (clonedPools.size > _maxUsersForEachPool) {
                clonedPools = clonedPools.subList(0, _maxUsersForEachPool)
            }

            val rewardLevelConfig = _rewardLevelConfig.toMap() // clone

            // Chia thuởng
            var fromIndex = 0
            poolLoop@ for ((raceLevel, levelConfig) in rewardLevelConfig) {
                val numberToPick = levelConfig.numUsers
                val toIndex = min(fromIndex + numberToPick, clonedPools.size)
                val pickedUsers = clonedPools.subList(fromIndex, toIndex)
                val reward = levelConfig.amountReward[rewardType]!!
                userLoop@ for (userHero in pickedUsers) {
                    if (rewardRemaining - reward < 0) {
                        break@poolLoop // Ko xử lý pool này nữa
                    }
                    rewardRemaining -= reward
                    val userId = userHero.userId

                    val ticketId = getTicketId(userId.dataType, userHero.hero)

                    if (!result.containsKey(userId)) {
                        result[userId] = mutableListOf()
                    }

                    val heroes = result[userId]!! as MutableList<RewardResult>
                    // FIXME: reward.toFloat() -> must be Double
                    val rewardDetail =
                        RewardDetail(rewardType, EnumConstants.MODE.TH_MODE_V2, userId.dataType, reward.toFloat())
                    heroes.add(RewardResult(userHero, raceLevel, rewardDetail))

                    consoleLog("Hero $ticketId (stake: ${getScore(userHero)}) (lv $raceLevel): $reward")
                }

                fromIndex = toIndex
                if (fromIndex == clonedPools.size) {
                    break@poolLoop
                }
            }

            consoleLog("------Pool ${POOL_NAMES[poolIndex]} end: ${rewardRemaining}-------")
            setRemainingReward(poolIndex, rewardRemaining)
        }
        return result.toMap()
    }

    private fun getScore(userHero: UserHero): Double {
        var score = 0.0
        val hero = userHero.hero
        if (rewardType == BLOCK_REWARD_TYPE.SENSPARK) {
            score = hero.stakeSen
        } else {
            if (!hero.isHeroS) {
                score = hero.stakeBcoin - _minStakeHeroConfig[hero.rarity]!!
            } else {
                score = hero.stakeBcoin
            }
        }
        if (score <= 0) {
            score = 0.1
        }
        return score * userHero.ticketCount
    }

    private fun getTicketId(dataType: DataType, hero: Hero): String {
        return "${dataType}:${hero.heroId}"
    }

    private fun getRemainingReward(poolIndex: Int): Double {
        return treasureHuntV2Config.rewardPool[poolIndex]!!
    }

    private fun setRemainingReward(poolIndex: Int, value: Double) {
        treasureHuntV2Config.rewardPool[poolIndex] = value
    }

    private fun consoleLog(message: String) {
        if (!_log) {
            return
        }
        _logger.log(message)
    }
}