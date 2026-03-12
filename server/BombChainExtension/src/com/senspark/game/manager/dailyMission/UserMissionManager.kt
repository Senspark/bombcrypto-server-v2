package com.senspark.game.manager.dailyMission

import com.senspark.game.api.IVerifyAdApiManager
import com.senspark.game.constant.ItemType
import com.senspark.game.constant.ItemType.*
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.manager.dailyMission.IMissionManager
import com.senspark.game.data.manager.hero.IConfigHeroTraditionalManager
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.manager.season.IPvpSeasonManager
import com.senspark.game.data.model.config.MissionReward
import com.senspark.game.data.model.user.AddUserItemWrapper
import com.senspark.game.data.model.user.UserMission
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IRewardDataAccess
import com.senspark.game.db.dailyMission.IMissionDataAccess
import com.senspark.game.declare.customEnum.MissionAction
import com.senspark.game.declare.customEnum.MissionRewardType
import com.senspark.game.declare.customEnum.MissionType
import com.senspark.game.exception.CustomException
import java.time.Instant
import java.time.temporal.ChronoUnit

class UserMissionManager(
    private val _mediator: UserControllerMediator,
    private val verifyAdApi: IVerifyAdApiManager,
    private val reloadReward: () -> Unit
) : IUserMissionManager {

    private val missionManager = _mediator.svServices.get<IMissionManager>()
    private val configHeroTraditionalManager = _mediator.svServices.get<IConfigHeroTraditionalManager>()
    private val configProductManager = _mediator.svServices.get<IConfigItemManager>()
    private val pvpSeasonManager = _mediator.svServices.get<IPvpSeasonManager>()
    
    private val dataAccessManager = _mediator.services.get<IDataAccessManager>()
    private val rewardDataAccess: IRewardDataAccess = dataAccessManager.rewardDataAccess
    private val missionDataAccess: IMissionDataAccess = dataAccessManager.missionDataAccess
        
    private var timeBeginReloadMission = Instant.now().truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS)
    private var _mission : Map<String, UserMission>? = null

    override fun getTodayMissions(): Map<String, UserMission> {
        if (_mission == null) {
            loadMission()
        }
        return _mission!!
    }

    override fun getMissions(type: MissionType): List<UserMission> {
        return getTodayMissions().values.filter { it.type == type }.toList()
    }
    
    override fun completeMission(actions: List<Pair<MissionAction, Int>>) {
        missionManager.completeMission(_mediator.userId, actions)
        loadMission()
    }

    override suspend fun watchAds(missionCode: String, adsToken: String) {
        val action = MissionAction.WATCH_ADS
        val isValid = verifyAdApi.isValidAds(adsToken)
        if (!isValid) {
            throw CustomException("Ads invalid")
        }
        val missionConfig = missionManager.getMission(missionCode)
        if (missionConfig.action != action) {
            throw CustomException("Mission invalid")
        }
        missionConfig.previousMissionCode?.let {
            val previousMission = getMission(it)
            if (!previousMission.isCompleted) {
                throw CustomException("The mission ${previousMission.missionCode} has not been completed")
            }
        }
        val userMission = getMission(missionCode)
        if (!userMission.coolDownEnded) {
            throw CustomException("Please wait ${userMission.remainCoolDown}")
        }
        missionDataAccess.saveCompleteMission(
            _mediator.userId,
            missionConfig.type,
            missionConfig.code,
            missionConfig.numberMission,
            1
        )
        loadMission()
    }

    override fun takeReward(missionCode: String): List<AddUserItemWrapper> {
        val mission = getMission(missionCode)
        if (!mission.isCompleted) {
            throw CustomException("The mission has not been completed")
        } else {
            if (mission.isReceivedReward) {
                throw CustomException("Reward has been received")
            }
        }
        val rewardsReceive = calculateReward(mission.rewards)
        rewardDataAccess.addTRRewardForUser(_mediator.userId, _mediator.dataType, rewardsReceive, reloadReward, "Daily_mission")
        missionDataAccess.saveReceivedReward(_mediator.userId, missionCode, rewardsReceive)
        return rewardsReceive
    }

    private fun loadMission() {
        missionDataAccess.checkUserAchievement(_mediator.userId, pvpSeasonManager.currentRewardSeasonNumber)
        val missionsConfig = missionManager.missionsList
        val userMissions = missionDataAccess.loadMission(_mediator.userId)
        _mission = missionsConfig.map {
            userMissions[it.code]?.apply {
                description = it.description
                action = it.action
                sort = it.sort
                rewards = it.reward
            } ?: UserMission(
                uid = _mediator.userId,
                type = it.type,
                missionCode = it.code,
                numberMission = it.numberMission,
                description = it.description,
                action = it.action,
                sort = it.sort,
                rewards = it.reward
            )
        }.map {
            missionManager.getPreviousMission(it.missionCode)?.let { prevMissionConfig ->
                val previousMission = userMissions[prevMissionConfig.code]
                it.calculateCoolDown(prevMissionConfig.coolDown, previousMission?.modifyDate)
            }
            it
        }.associateBy { it.missionCode }
    }

    private fun getMission(missionCode: String): UserMission {
        val now = Instant.now()
        if (now.isAfter(timeBeginReloadMission)) {
            loadMission()
            timeBeginReloadMission = timeBeginReloadMission.plus(1, ChronoUnit.DAYS)
        }
        return getTodayMissions()[missionCode] ?: throw CustomException("Cannot find mission $missionCode")
    }

    private fun calculateReward(rewards: List<MissionReward>): List<AddUserItemWrapper> {
        val rewardsReceive = mutableListOf<AddUserItemWrapper>()
        rewards.forEach { reward ->
            when (reward.type) {
                MissionRewardType.RANDOM_ALL -> {
                    randomAllSkin(rewardsReceive, reward, reward.itemType)
                }

                MissionRewardType.RANDOM_IN_LIST -> {
                    reward.getRandomReward()?.apply {
                        val item = configProductManager.getItem(this.itemId)
                        rewardsReceive.add(
                            AddUserItemWrapper(
                                item,
                                reward.quantity,
                                reward.isLock,
                                reward.expirationAfter,
                                userId = _mediator.userId,
                                configHeroTraditional = configHeroTraditionalManager
                            )
                        )
                    }
                }

                MissionRewardType.TAKE_ALL_IN_LIST -> {
                    reward.rewards.forEach {
                        val item = configProductManager.getItem(it.itemId)
                        rewardsReceive.add(
                            AddUserItemWrapper(
                                item,
                                it.quantity,
                                userId = _mediator.userId,
                                configHeroTraditional = configHeroTraditionalManager
                            )
                        )
                    }
                }
            }
        }
        return rewardsReceive
    }

    private fun randomAllSkin(
        rewardsReceive: MutableList<AddUserItemWrapper>,
        reward: MissionReward,
        itemType: ItemType
    ) {
        when (itemType) {
            BOMB, WING, TRAIL, FIRE -> {
                configProductManager.getRandom(itemType)?.apply {
                    rewardsReceive.add(
                        AddUserItemWrapper(
                            this,
                            reward.quantity,
                            reward.isLock,
                            reward.expirationAfter,
                            userId = _mediator.userId,
                            configHeroTraditional = configHeroTraditionalManager
                        )
                    )
                }
            }

            else -> throw CustomException("Product type $itemType invalid")
        }
    }
}