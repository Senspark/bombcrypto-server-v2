package com.senspark.game.manager.ton

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.treassureHunt.ICoinRankingManager
import com.senspark.game.data.model.config.TonTasksConfig
import com.senspark.game.db.ITHModeDataAccess
import com.senspark.game.exception.CustomException
import com.senspark.lib.data.manager.GameConfigManager
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.SFSArray

enum class TaskType {
    BuyHero,
    BuyHouse,
    OpenLink
    ;

    companion object {
        fun fromInt(value: Int): TaskType? {
            return entries.find { it.ordinal == value }
        }
    }
}

class TonTasksManager(
    private val thModeDataAccess: ITHModeDataAccess,
    private val coinRankingManager: ICoinRankingManager,
    private val gameConfigManager: IGameConfigManager,
) : ITasksManager {

    private var tasksConfig: MutableMap<Int, TonTasksConfig> = mutableMapOf()

    override fun initialize() {
        tasksConfig = thModeDataAccess.getTasksConfig()
    }

    override fun setConfig(config: MutableMap<Int, TonTasksConfig>) {
        tasksConfig = config
    }

    override fun getUserTasks(userController: IUserController): SFSArray {
        val userCompletedTasks = thModeDataAccess.getUserCompletedTasks(userController.userId)
        val result = SFSArray()
        for ((taskId, task) in tasksConfig) {
            var isCompleted: Boolean
            var isClaimed = false

            if (userCompletedTasks.containsKey(taskId)) {
                isCompleted = true
                if (userCompletedTasks[taskId] == true) {
                    isClaimed = true
                }
            } else {
                isCompleted = when (task.type) {
                    TaskType.BuyHero, TaskType.BuyHouse -> completeTask(userController, taskId)
                    else -> false
                }
            }
            result.addSFSObject(task.toSFSObject().apply {
                putBool("is_completed", isCompleted)
                putBool("is_claimed", isClaimed)
            })
        }
        return result
    }

    override fun completeTask(userController: IUserController, taskId: Int): Boolean {
        if (!tasksConfig.containsKey(taskId)) {
            return false
        }

        val isCompleted = when (tasksConfig[taskId]!!.type) {
            TaskType.BuyHero -> when (taskId) {
                1 -> checkBuyHero(userController, 1)
                2 -> checkBuyHero(userController, 5)
                3 -> checkBuyHero(userController, 15)
                else -> false
            }

            TaskType.BuyHouse -> checkBuyHouse(userController)
            TaskType.OpenLink -> true
        }

        if (isCompleted) {
            thModeDataAccess.saveTaskComplete(userController.userId, taskId)
        }
        return isCompleted
    }

    override fun claimTaskReward(userController: IUserController, taskId: Int) {
        if (!tasksConfig.containsKey(taskId)) {
            throw CustomException("Not exist task")
        }

        val reward = tasksConfig[taskId]!!.reward
        thModeDataAccess.chaimTask(userController.userId, taskId, reward.toDouble())
        coinRankingManager.saveRankingCoin(userController.userId, reward.toFloat(), userController.dataType)
    }

    private fun checkBuyHero(userController: IUserController, quantity: Int): Boolean {
        val defaultHero = gameConfigManager.newUserTonGiftHero.size
        return userController.masterUserManager.heroFiManager.tonHeroes.size - defaultHero >= quantity
    }

    private fun checkBuyHouse(userController: IUserController): Boolean {
        return userController.masterUserManager.houseManager.getUserHouses().isNotEmpty()
    }
}