package com.senspark.game.manager.dailyTask

import com.senspark.game.data.model.config.IGachaChestItem
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray

class NullUserDailyTaskManager : IUserDailyTaskManager {
    override fun updateProgressTask(taskIds: List<Int>, amount: Int) {}

    override fun claimTaskReward(taskId: Int): List<ItemForClaim>? {
        return null
    }

    override fun getUserTodayTask(): TodayTask {
        return TodayTask(
            finalRewardClaimed = false,
            tasks = mutableListOf(),
            urlConfig = ""
        )
    }

    override fun getUserDailyProgress(): List<Int> {
        return emptyList()
    }

    override fun saveToDatabase() {}
    override fun claimFinalReward(): List<IGachaChestItem> {
        return mutableListOf()
    }

}