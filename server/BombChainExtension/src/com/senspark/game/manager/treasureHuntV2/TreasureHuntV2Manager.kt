package com.senspark.game.manager.treasureHuntV2

import com.senspark.common.cache.IMessengerService
import com.senspark.common.utils.ILogger
import com.senspark.common.utils.toSFSArray
import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.config.TreasureHuntV2Config
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.user.RewardDetail
import com.senspark.game.db.ITHModeDataAccess
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.manager.stake.IHeroStakeManager
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import com.smartfoxserver.v2.extensions.SFSExtension
import java.util.*

class TreasureHuntV2Manager(
    private val _thModeDataAccess: ITHModeDataAccess,
    logger: ILogger,
    messengerService: IMessengerService,
    private val _heroStakeManager: IHeroStakeManager,
    extension: SFSExtension?,
) : ITreasureHuntV2Manager {
    private val _thModeRaceBroadcaster: THModeRaceBroadcaster = THModeRaceBroadcaster(messengerService)
    override var period = 60

    private var treasureHuntV2Config: Map<BLOCK_REWARD_TYPE, TreasureHuntV2Config>
    private var maxPool: Map<BLOCK_REWARD_TYPE, Map<Int, Double>>
    private var listCalculateRewardManagers: MutableMap<BLOCK_REWARD_TYPE, CalculateRewardManager> = mutableMapOf()
    private val room: ITHModeV2Room
    private var raceId: Int = 0
    private var isStopPool: Boolean = false
    
    init {
        treasureHuntV2Config = _thModeDataAccess.loadTHModeV2Config()
        maxPool = _thModeDataAccess.loadMaxTHModeV2Pool()
        period = treasureHuntV2Config[BLOCK_REWARD_TYPE.BCOIN]?.period ?: 60

        val rewardLevelConfig = _thModeDataAccess.loadRewardLevelConfig()
        for (config in treasureHuntV2Config) {
            listCalculateRewardManagers[config.key] =
                CalculateRewardManager(
                    config.value, config.key, _thModeRaceBroadcaster, _heroStakeManager, rewardLevelConfig, logger
                )
        }

        room = if (extension != null)
            THModeV2Room(period, maxPoolToArray(), logger, extension)
        else NullTHModeV2Room()
        raceId = _thModeDataAccess.getNextRaceId()
    }

    /**
     * For testing only
     */
    fun setListCalculateRewardManagers(listCalculateRewardManagers: MutableMap<BLOCK_REWARD_TYPE, CalculateRewardManager>) {
        this.listCalculateRewardManagers = listCalculateRewardManagers
    }

    override fun setStopPool(stop: Boolean) {
        isStopPool = stop
    }

    override fun initialize() {
    }

    override fun reloadConfigs() {
        treasureHuntV2Config = _thModeDataAccess.loadTHModeV2Config()
        val rewardLevelConfig = _thModeDataAccess.loadRewardLevelConfig()

        for (config in treasureHuntV2Config) {
            listCalculateRewardManagers[config.key]?.setConfig(config.value, rewardLevelConfig)
        }
        period = treasureHuntV2Config[BLOCK_REWARD_TYPE.BCOIN]?.period ?: 60
        room.updateConfigVariable(period, maxPoolToArray())
    }

    override fun joinRoom(user: IUserController) {
        room.joinRoom(user)
    }

    override fun leaveRoom(user: IUserController) {
        room.leaveRoom(user)
    }

    override fun addHeroToPool(hero: Hero, userId: UserId): List<Int> {
        val attendPools: MutableList<Int> = mutableListOf()
        if (isStopPool) {
            return attendPools
        }
        val minStakeHeroConfig = _heroStakeManager.minStakeHeroConfig
        val minStakeBcoin = treasureHuntV2Config[BLOCK_REWARD_TYPE.BCOIN]?.minStake?.get(hero.rarity)
        val minStakeSen = treasureHuntV2Config[BLOCK_REWARD_TYPE.SENSPARK]?.minStake?.get(hero.rarity)

        var stakeBcoin = hero.stakeBcoin
        if (!hero.isHeroS) {
            stakeBcoin -= minStakeHeroConfig[hero.rarity]!!
        }
        if (minStakeBcoin != null && stakeBcoin >= minStakeBcoin) {
            listCalculateRewardManagers[BLOCK_REWARD_TYPE.BCOIN]?.addHeroToPool(hero, userId, raceId)
            attendPools.add(BLOCK_REWARD_TYPE.BCOIN.value)
        }

        if (minStakeSen != null && hero.stakeSen >= minStakeSen) {
            listCalculateRewardManagers[BLOCK_REWARD_TYPE.SENSPARK]?.addHeroToPool(hero, userId, raceId)
            attendPools.add(BLOCK_REWARD_TYPE.SENSPARK.value)
        }
        return attendPools
    }

    override fun calculateReward(): Map<UserId, List<MultipleRewardResult>> {
        val result: MutableMap<UserId, MutableMap<Hero, MultipleRewardResult>> = mutableMapOf()
        //tạm dừng pool để update thông số
        if (isStopPool) {
            return result.mapValues { entry -> entry.value.values.toList() }.toMap()
        }

        // Gộp kết quả theo [UserController, [Hero, Reward]]
        for (calRewardManager in listCalculateRewardManagers) {
            val maps: Map<UserId, List<RewardResult>> = calRewardManager.value.calculateReward()

            for ((usrCtrl, rewards) in maps) {
                if (!result.containsKey(usrCtrl)) {
                    result[usrCtrl] = mutableMapOf()
                }
                val heroes = result[usrCtrl]!!

                for (reward in rewards) {
                    val hero = reward.userHero.hero
                    if (!heroes.containsKey(hero)) {
                        heroes[hero] = MultipleRewardResult(hero, reward.rewardLevel, mutableListOf())
                    }
                    val combinedRewards = heroes[hero]!!.reward as MutableList
                    combinedRewards.add(reward.reward)
                }
            }
        }
        raceId = _thModeDataAccess.getNextRaceId()
        //Chuyển qua dùng stream ko cần update raceid nữa
        //cachedTHModeDataAccess.updateRaceId(raceId)
        val output = result.mapValues { entry -> entry.value.values.toList() }.toMutableMap()
        _thModeDataAccess.writeLogTHModeRewards(raceId, output)
        return output
    }

    override fun sumReward(
        rewardList: List<MultipleRewardResult>
    ): Map<BLOCK_REWARD_TYPE, RewardDetail> {
        val mapReward: MutableMap<BLOCK_REWARD_TYPE, RewardDetail> = EnumMap(BLOCK_REWARD_TYPE::class.java)
        val allRewards = rewardList.flatMap { it.reward }
        val rewardsByType = allRewards.groupBy { it.blockRewardType }

        for ((type, rewards) in rewardsByType) {
            val sum = rewards.sumOf { it.value.toDouble() }
            val any = rewards.first()
            mapReward[type] = RewardDetail(type, any.mode, any.dataType, sum.toFloat())
        }
        return mapReward
    }

    override fun getRewardDetail(rewardList: List<MultipleRewardResult>): Map<BLOCK_REWARD_TYPE, Map<Int, List<RewardDetail>>> {
        val relevantTypes = listOf(BLOCK_REWARD_TYPE.BCOIN, BLOCK_REWARD_TYPE.SENSPARK, BLOCK_REWARD_TYPE.COIN)
        val mapRewardDetail =
            EnumMap<BLOCK_REWARD_TYPE, MutableMap<Int, ArrayList<RewardDetail>>>(BLOCK_REWARD_TYPE::class.java)
        relevantTypes.forEach { mapRewardDetail[it] = mutableMapOf() }

        for (reward in rewardList) {
            val rarity = reward.hero.rarity
            relevantTypes.forEach { type ->
                mapRewardDetail[type]?.getOrPut(rarity) { arrayListOf() }?.addAll(
                    reward.reward.filter { it.blockRewardType == type }
                )
            }
        }

        return mapRewardDetail
    }

    override fun saveRewardPool() {
        if (isStopPool) {
            return
        }
        for (calRewardManager in listCalculateRewardManagers) {
            _thModeDataAccess.updateTHModeV2Pool(
                calRewardManager.value.treasureHuntV2Config.rewardPool,
                calRewardManager.key
            )
        }
        room.updateRewardPoolVariable(allPoolToSFSArray())
    }

    override fun refillRewardPool() {
        _thModeDataAccess.refillTHModeV2Pool()
        val configs = treasureHuntV2Config
        for (config in configs) {
            config.value.rewardPool = maxPool[config.key]!!.mapValues { it.value}.toMutableMap()
        }
        val now = Calendar.getInstance()
        now.add(Calendar.DAY_OF_MONTH, 1)
        room.updateRewardPoolVariable(allPoolToSFSArray())
        room.updateTimeRefillPoolVariable(now.toInstant().epochSecond)
    }

    private fun maxPoolToArray(): ISFSArray {
        val array = SFSArray()
        for ((type, config) in treasureHuntV2Config) {
            array.addSFSObject(SFSObject().apply {
                putInt("block_type", type.value)
                putInt("max_reward", config.maxPool)
            })
        }
        return array
    }

    override fun allPoolToSFSArray(): ISFSArray {
        val result = mutableMapOf<Int, MutableMap<BLOCK_REWARD_TYPE, Double>>()

        for ((type, config) in treasureHuntV2Config) {
            for ((id, reward) in config.rewardPool) {
                val typeMap = result.getOrPut(id) { mutableMapOf() }
                typeMap[type] = reward
            }
        }

        val array = SFSArray()
        for ((poolId, rewardPoolByType) in result) {
            val obj = SFSObject()
            obj.putSFSArray("reward_by_type", rewardPoolByType.toSFSArray {
                SFSObject().apply {
                    putInt("block_type", it.key.value)
                    putDouble("remaining_reward", it.value)
                    putInt("max_pool", maxPool[it.key]!![poolId]!!.toInt())
                }
            })
            obj.putInt("pool_id", poolId)
            array.addSFSObject(obj)
        }

        return array
    }
}