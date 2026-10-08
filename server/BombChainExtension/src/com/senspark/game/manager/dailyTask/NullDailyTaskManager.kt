package com.senspark.game.manager.dailyTask

class NullDailyTaskManager : IDailyTaskManager {

    override fun initialize() {
    }

    override fun checkCacheAndChangeTask() {
    }

    override fun getTodayTask(): List<DailyTask> {
        return emptyList()
    }

    override fun hotReloadTodayTask(taskIds: List<Int>) {
    }

    override fun hotReloadConfigTask() {
    }
}
