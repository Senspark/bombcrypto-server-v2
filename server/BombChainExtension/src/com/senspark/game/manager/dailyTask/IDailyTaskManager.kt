package com.senspark.game.manager.dailyTask

import com.senspark.common.service.IServerService

interface IDailyTaskManager : IServerService {
    fun checkCacheAndChangeTask()
    fun getTodayTask(): List<DailyTask>
    
    // Admin command
    fun hotReloadTodayTask(taskIds: List<Int> = emptyList())
    fun hotReloadConfigTask()
}