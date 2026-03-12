package com.senspark.game.manager.offlineReward

import com.senspark.game.api.IVerifyAdApiManager
import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.hero.IConfigHeroTraditionalManager
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.model.user.AddUserItemWrapper
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.db.IRewardDataAccess
import com.senspark.game.extension.coroutines.ICoroutineScope
import com.senspark.game.utils.JsonExtensionBuilder
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.temporal.ChronoUnit

typealias OfflineHours = Int
internal typealias ItemId = Int
internal typealias Quantity = Int

class OfflineRewardManager(
    private val _rewardDataAccess: IRewardDataAccess,
    private val _gameDataAccess: IGameDataAccess,
    private val _configItemManager: IConfigItemManager,
    private val _configHeroTraditionalManager: IConfigHeroTraditionalManager,
    private val _verifyAdApi: IVerifyAdApiManager,
) : IOfflineRewardManager {

    /**
     * list of OfflineRewardConfig sorted descending by offlineHours
     */
    private val _configs: MutableList<OfflineRewardConfig> = mutableListOf()

    override fun initialize() {
        _configs.addAll(loadConfig())
    }
    
    private fun loadConfig(): List<OfflineRewardConfig> {
        val configs = _gameDataAccess.getOfflineRewardConfigs()
        return configs.map { OfflineRewardConfig(it.key, it.value) }.sortedByDescending { it.offlineHours }.toList()
    }

    override fun claimRewards(userController: IUserController): Pair<Int, Map<ItemId, Int>> {
        val rewards = getRewards(userController)
        if (rewards.second.isNotEmpty()) {
            processGiveRewards(userController, rewards.second)
        }
        return rewards
    }

    override suspend fun claimRewardsWithAds(
        userController: IUserController,
        adsToken: String
    ): Pair<OfflineHours, Map<ItemId, Int>> {
        val rewards = getRewards(userController)
        if (rewards.second.isEmpty()) return rewards
        val isValid = _verifyAdApi.isValidAds(adsToken)
        if (!isValid)
            throw Exception("Invalid token: $adsToken")
        val x2Rewards = rewards.second.map { it.key to it.value * 2 }.toMap()
        processGiveRewards(userController, x2Rewards)
        return Pair(rewards.first, x2Rewards)
    }

    private fun processGiveRewards(userController: IUserController, rewards: Map<ItemId, Quantity>) {
        val wrapRewards = rewards.map {
            val item = _configItemManager.getItem(it.key)
            AddUserItemWrapper(
                item = item,
                quantity = it.value,
                userId = userController.userId,
                configHeroTraditional = _configHeroTraditionalManager
            )
        }.toList()
        transaction {
            _rewardDataAccess.addTRRewardForUser(
                userController.userId,
                userController.dataType,
                wrapRewards,
                { onGiveRewardSuccess(userController) },
                "Offline_reward")
            userController.masterUserManager.userConfigManager.apply {
                miscConfigs.lastReceiveOfflineReward = Instant.now().toEpochMilli()
                saveMiscConfigs()
            }
        }
    }

    private fun onGiveRewardSuccess(userController: IUserController) {
        userController.masterUserManager.blockRewardManager.loadUserBlockReward()
    }

    override fun getRewards(userController: IUserController): Pair<OfflineHours, Map<ItemId, Int>> {
        val lastLogout = userController.userInfo.lastLogout ?: return Pair(0, emptyMap())
        if (hasAlreadyReceiveRewardToday(userController))
            return Pair(0, emptyMap())
        val offlineHours = (Instant.now().epochSecond - lastLogout.epochSecond) / 60 / 60
        val config = _configs.firstOrNull { it.offlineHours <= offlineHours } ?: return Pair(0, emptyMap())
        val bonusFromSubscription = userController.masterUserManager.userSubscriptionManager.offlineRewardBonus
        return Pair(config.offlineHours, config.rewards.mapValues { it.value + it.value * bonusFromSubscription })
    }

    private fun hasAlreadyReceiveRewardToday(userController: IUserController): Boolean {
        val userConfigManager = userController.masterUserManager.userConfigManager
        val lastReceiveTime = Instant.ofEpochMilli(userConfigManager.miscConfigs.lastReceiveOfflineReward)
        return lastReceiveTime.truncatedTo(ChronoUnit.DAYS) == Instant.now().truncatedTo(ChronoUnit.DAYS)
    }
}

class OfflineRewardConfig(
    val offlineHours: Int,
    private val rewardsJson: String
) {
    val rewards: Map<ItemId, Quantity> = JsonExtensionBuilder.json.parseToJsonElement(rewardsJson).jsonArray.associate {
        val itemId = it.jsonObject.getValue("item_id").jsonPrimitive.int
        val quantity = it.jsonObject.getValue("quantity").jsonPrimitive.int
        itemId to quantity
    }
}
