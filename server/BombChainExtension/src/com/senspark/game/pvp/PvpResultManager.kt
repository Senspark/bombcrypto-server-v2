package com.senspark.game.pvp

import com.senspark.game.api.IPvpResultInfo
import com.senspark.game.constant.Booster
import com.senspark.game.controller.IUserController
import com.senspark.game.data.RewardData
import com.senspark.game.data.manager.dailyMission.IMissionManager
import com.senspark.game.data.manager.gacha.IGachaChestSlotManager
import com.senspark.game.data.manager.pvp.IPvpRankingManager
import com.senspark.game.data.manager.season.IPvpSeasonManager
import com.senspark.game.data.model.user.UserGachaChest
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.db.gachaChest.IGachaChestDataAccess
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.customEnum.ChangeRewardReason
import com.senspark.game.declare.customEnum.GachaChestType
import com.senspark.game.declare.customEnum.MissionAction
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IUsersManager
import com.senspark.game.manager.dailyTask.DailyTaskManager
import com.senspark.game.schema.TableUserBooster
import com.senspark.game.service.IPvpDataAccess
import com.senspark.game.user.IGachaChestManager
import com.senspark.game.user.ITrGameplayManager
import com.senspark.game.utils.IUserFinder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select

class PvpResultManager(
    private val _envManager: IEnvManager,
    private val _userDataAccess: IUserDataAccess,
    private val _gachaChestDataAccess: IGachaChestDataAccess,
    private val _gachaChestManager: IGachaChestManager,
    private val _gachaChestSlotManager: IGachaChestSlotManager,
    private val _userFinder: IUserFinder,
    private val _pvpDataAccess: IPvpDataAccess,
    private val _pvpSeasonManager: IPvpSeasonManager,
    private val _missionManager: IMissionManager,
    private val _usersManager: IUsersManager,
    private val _trGameplayManger: ITrGameplayManager,
    private val _pvpRankingManger: IPvpRankingManager
) : IPvpResultManager {
    
    private class MatchReward(
        override val rewardId: String,
        override val isOutOfChestSlot: Boolean,
    ) : IPvpMatchReward

    // Save on memory.
    private val _userRewards = mutableMapOf<Int, IPvpMatchReward>()

    override fun initialize() {
    }

    override fun claimReward(userId: Int): IPvpMatchReward? {
        return _userRewards.remove(userId)
    }

    override fun handleResult(info: IPvpResultInfo) {
        if (info.rule.isTournament) {
            handleTournamentResult(info)
        } else {
            info.info.forEachIndexed { index, it ->
                if (it.isBot) {
                    // Ignore bot.
                    return@forEachIndexed
                }
                if (it.serverId != _envManager.serverId) {
                    // Ignore user from other servers.
                    return@forEachIndexed
                }
                handle(info, index)
            }
        }
    }

    private fun handle(info: IPvpResultInfo, slot: Int) {
        val seasonId = _pvpSeasonManager.currentSeasonNumber
        val userInfo = info.info[slot]
        val username = userInfo.username
        val user = _userFinder.find(username)
        val dataType = _trGameplayManger.getCurrentTypePlayingPvp(userInfo.userId)

        var controller: IUserController? = null
        // Since multiple controllers are in use, we must retrieve the correct controller to perform the update accurately.
        if (user != null) {
            controller = if(dataType == null) {
                _usersManager.getUserController(userInfo.userId)
            } else{
                _usersManager.getUserController(userInfo.userId, dataType)
            }
        }

        // Synchronize with the database before updating PVP points
        controller?.reloadPvpRanking()


        val rewardId = "${info.id}:${userInfo.userId}"
        var isOutOfChestSlot = false
        val rewards = userInfo.rewards.toMutableMap()
        val completedAction = mutableListOf<Pair<MissionAction, Int>>()
        val shieldsUsed = userInfo.usedBoosters[Booster.Shield.value] ?: 0
        val keysUsed = userInfo.usedBoosters[Booster.Key.value] ?: 0
        // temporarily disabled until requested
        //check if no match played before add 30 GEM_LOCKED
//        if (_userDataAccess.countPvpPlayedMatch(userInfo.userId) == 0) {
//            rewards.compute(BLOCK_REWARD_TYPE.GEM_LOCKED.value) { _, v ->
//                v?.plus(30f) ?: 30f
//            }
//        }
        if (userInfo.teamId == info.winningTeam) {
            completedAction.add(Pair(MissionAction.WIN_PVP, 1))
            // Delay 2 seconds to avoid race conditions where client receives PVP_FINISH_MATCH after entry.finish
            controller?.masterUserManager?.userDailyTaskManager?.updateProgressTask(DailyTaskManager.PlayPvpWin)
        }
        if (shieldsUsed > 0) {
            // Using shield in PVP, check and update daily task
            controller?.masterUserManager?.userDailyTaskManager?.updateProgressTask(DailyTaskManager.UseShieldInPvp, shieldsUsed)
            completedAction.add(Pair(MissionAction.USE_SHIELD, shieldsUsed))
        }
        if (keysUsed > 0) {
            // Using key in PVP, check and update daily task
            controller?.masterUserManager?.userDailyTaskManager?.updateProgressTask(DailyTaskManager.UseKeyInPvp, keysUsed)
            completedAction.add(Pair(MissionAction.USE_KEY, keysUsed))
        }
        _pvpDataAccess.update {
            // Update user rank.
            _pvpDataAccess.updateUserRank(
                userInfo.userId,
                info.winningTeam == userInfo.teamId,
                userInfo.deltaPoint,
                seasonId,
            )
            // Operation failed
            _logger.error("[PvpResultManager] Failed to process result for user ${userInfo.username}")
//            TablePvPUserRank(seasonId).update(
//                userId = userInfo.userId,
//                isWinner = info.winningTeam == userInfo.teamId,
//                deltaPoint = userInfo.deltaPoint,
//            )
            if (controller == null) {
                _userDataAccess.increasePVPMatchCount(
                    userInfo.userId,
                    info.winningTeam == userInfo.teamId,
                    _gachaChestSlotManager
                )
                _missionManager.completeMission(userInfo.userId, completedAction)
            } else {
                controller.apply {
                    updatePvpRanking(userInfo.deltaPoint, 1, if (info.winningTeam == userInfo.teamId) 1 else 0)
                    masterUserManager.userConfigManager.miscConfigs.inCreatePvpMatchCount(info.winningTeam == userInfo.teamId)
                    masterUserManager.userConfigManager.saveMiscConfigs()
                    masterUserManager.userMissionManager.completeMission(completedAction)
                }
            }
            saveUsedBoosters(controller, userInfo.usedBoosters, userInfo.userId)
            if (info.isDraw) {
                // No chest.
                controller?.apply {
                    // The checkMatchId fails if the match started before the player left the queue.
                    _logger.log("[PvpResultManager] user ${userInfo.username} is in a different matchId (current: $matchId, received: ${info.id})")
                    masterUserManager.userBonusRewardManager.addRewardsAds(rewardId)
                }
            } else {
                // Update to the correct value.
                isOutOfChestSlot = saveRewardsAndGetGachaSlotStatus(controller, rewards, userInfo.userId, rewardId, info, dataType)
                _userRewards[userInfo.userId] = MatchReward(rewardId, isOutOfChestSlot)
            }
        }
    }

    private fun handleTournamentResult(info: IPvpResultInfo) {
        _userDataAccess.updateTournamentResult(info)
    }

    private fun saveUsedBoosters(controller: IUserController?, usedBoosters: Map<Int, Int>, userId: Int) {
        if (controller != null) {
            usedBoosters.forEach { entry ->
                val booster = controller.masterUserManager.userPvPBoosterManager.getBooster(entry.key)
                    ?: throw Exception("[SavePVPResult] Not found booster: ${entry.key}")
                repeat(entry.value) {
                    val userBoosterId = booster.listId.firstOrNull { !booster.listUsed.contains(it) }
                        ?: throw Exception("[SavePVPResult] Not enough booster: ${entry.key}")
                    booster.listUsed.add(userBoosterId)
                    booster.used = true
                }
            }
            controller.masterUserManager.userPvPBoosterManager.saveUsedBooster()
        } else {
            val boosterIds = mutableListOf<Int>()
            val select = TableUserBooster.select {
                (TableUserBooster.uid eq userId) and (TableUserBooster.itemId inList usedBoosters.map { it.key })
            }
            usedBoosters.forEach { entry ->
                repeat(entry.value) {
                    val booster = select.first {
                        it[TableUserBooster.itemId] == entry.key && !boosterIds.contains(it[TableUserBooster.id])
                    }
                    boosterIds.add(booster[TableUserBooster.id])
                }
            }
            _userDataAccess.subUserPvpBoosters(userId, boosterIds)
        }
    }

    private fun saveRewardsAndGetGachaSlotStatus(
        controller: IUserController?,
        rewards: Map<Int, Float>,
        userId: Int,
        rewardId: String,
        info: IPvpResultInfo,
        playingDataType: DataType?
    ): Boolean {
        val rewardsInternal = rewards.toMutableMap()
        if (rewardsInternal.isEmpty()) {
            controller?.apply {
                masterUserManager.userBonusRewardManager.addRewardsAds(rewardId)
            }
        }
        var outOfSlot = false
        rewardsInternal.forEach { rewardEntry ->
            val wagerToken = com.senspark.game.pvp.config.PvpWagerToken.from(info.wagerToken)
            val networkName = if (rewardEntry.key == wagerToken.rewardType.value && wagerToken != com.senspark.game.pvp.config.PvpWagerToken.NONE) {
                wagerToken.network.name
            } else {
                playingDataType?.name ?: DataType.TR.name
            }
            
            val finalReward = RewardData(
                rewardEntry.key,
                BLOCK_REWARD_TYPE.valueOf(rewardEntry.key).name,
                networkName,
                rewardEntry.value,
            )
            
            controller?.apply {
                masterUserManager.userBonusRewardManager.addRewardsAds(rewardId, finalReward)
            }
            
            val rewardType = BLOCK_REWARD_TYPE.valueOf(finalReward.id)
            if (rewardType.name.contains("chest", ignoreCase = true)) {
                outOfSlot = if (controller != null) {
                    controller.masterUserManager.userGachaChestManager.addChestFromBlockRewardType(rewardType) == null
                } else {
                    addChestFromBlockRewardTypeForDisconnectUser(
                        rewardType,
                        userId
                    ) == null
                }
            } else {
                _pvpDataAccess.updateUserReward(userId, mutableListOf(finalReward), ChangeRewardReason.REWARD_PVP)
            }
        }
        return outOfSlot
    }

    private fun addChestFromBlockRewardTypeForDisconnectUser(
        blockRewardType: BLOCK_REWARD_TYPE,
        userId: Int
    ): UserGachaChest? {
        val userConfigs = _userDataAccess.loadUserConfig(userId, _gachaChestSlotManager)
        val numberChestSlot = userConfigs.userGachaChestSlots.filter { it.isOwner }.size
        val chestType = when (blockRewardType) {
            BLOCK_REWARD_TYPE.BRONZE_CHEST -> GachaChestType.BRONZE
            BLOCK_REWARD_TYPE.SILVER_CHEST -> GachaChestType.SILVER
            BLOCK_REWARD_TYPE.GOLD_CHEST -> GachaChestType.GOLD
            BLOCK_REWARD_TYPE.PLATINUM_CHEST -> GachaChestType.PLATINUM
            else -> throw CustomException("Chest with type ${blockRewardType.name} invalid")
        }
        return _gachaChestDataAccess.addGachaChestForUser(userId, chestType, numberChestSlot, _gachaChestManager, true)
    }
}

