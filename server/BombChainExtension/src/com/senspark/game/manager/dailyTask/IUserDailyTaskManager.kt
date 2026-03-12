package com.senspark.game.manager.dailyTask

import com.senspark.game.data.model.config.IGachaChestItem
import com.smartfoxserver.v2.entities.data.ISFSArray

interface IUserDailyTaskManager {
    fun updateProgressTask(taskIds: List<Int>, amount: Int = 1)
    fun claimTaskReward(taskId: Int) : List<ItemForClaim>?
    fun getUserTodayTask(): TodayTask
    fun getUserDailyProgress(): List<Int>
    fun saveToDatabase()
    fun claimFinalReward(): List<IGachaChestItem>
}