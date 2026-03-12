package com.senspark.game.manager.dailyTask

import com.senspark.common.cache.ICacheService
import com.senspark.common.utils.ILogger
import com.senspark.game.constant.CachedKeys
import com.senspark.game.db.IUserDataAccess
import com.senspark.lib.data.manager.IGameConfigManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class DailyTaskManager(
    private val userDataAccess: IUserDataAccess,
    private val cache: ICacheService,
    private val _logger: ILogger,
    private val _gameConfigManager: IGameConfigManager,
) : IDailyTaskManager {
    private lateinit var todayTask: List<DailyTask>
    private lateinit var configTask: List<DailyTask>
    private var numberTaskInDay: Int = 5

    companion object TaskMapWithId {
        val PlayAdventure: List<Int> = listOf(1, 2)
        val PlayPvpWin: List<Int> = listOf(3)
        val BuyItemP2P: List<Int> = listOf(4)
        val BuyHeroP2P: List<Int> = listOf(5)
        val GrindHero: List<Int> = listOf(6)
        val UpgradeHero: List<Int> = listOf(7)
        val UseShieldInPvp: List<Int> = listOf(8)
        val UseKeyInPvp: List<Int> = listOf(9)
        val DefeatBossInAdventure: List<Int> = listOf(10)
    }

    override fun initialize() {
        loadConfig()
        checkCacheAndChangeTask()
    }

    private fun loadConfig() {
        configTask = userDataAccess.getDailyTaskConfig()
        configTask.forEach {
            it.reward = parseReward(it.rewardString)
        }
        numberTaskInDay = _gameConfigManager.totalTaskInDay

    }

    private fun getTaskByCache(): List<DailyTask> {
        try {
            val result = cache.get(CachedKeys.SV_TODAY_TASK)?.let {
                Json.decodeFromString<CachedTask>(it)
            }
            if (result == null || !isSameDate(result.date) || result.tasks.isEmpty()) {
                return emptyList()
            } else {
                // Có sự thay đổi số lượng task 1 ngày, cần update lại
                if (result.tasks.size != numberTaskInDay) {
                    _logger.error("Number task in cache ${result.tasks.size} is not equal to number task in config ${numberTaskInDay}, random new task")
                    return emptyList()
                }

                val config = configTask.filter { result.tasks.contains(it.id) }
                // Nếu có task bị xóa thì trả về empty list để random lại 5 task mới
                if (config.any { it.isDeleted }) {
                    _logger.error("Some task in cache is deleted, random new task")
                    return emptyList()
                }

                return config
            }
        } catch (e: Exception) {
            _logger.error("Error get task by cache")
            return emptyList()
        }
    }

    // Call by scheduler or call when server start and cache is empty
    override fun checkCacheAndChangeTask() {
        // Xem cache có lưu task hôm nay không
        val cacheTask = getTaskByCache()
        if (cacheTask.isEmpty()) {
            changeTask()
        } else {
            todayTask = cacheTask
        }
    }

    private fun changeTask() {
        // Loại bỏ các task đã bị xóa
        val exitedTasks = configTask.filter { !it.isDeleted }
        // Lấy các task mặc định
        val defaultTasks = exitedTasks.filter { it.isDefault }
        val remainingTasks = exitedTasks.minus(defaultTasks.toSet())

        // Các task còn lại sau khi trừ đi task mặc định
        var numberTaskLeft = numberTaskInDay - defaultTasks.size

        // Thêm số task random từ các task còn lại
        numberTaskLeft = minOf(numberTaskLeft, remainingTasks.count())
        numberTaskLeft = maxOf(numberTaskLeft, 0)
        val randomSelection = remainingTasks.shuffled().take(numberTaskLeft)

        todayTask = defaultTasks + randomSelection
        todayTask = todayTask.sortedBy { it.id }

        saveToCacheAndLog()
    }

    private fun saveToCacheAndLog() {
        try {
            // Lưu lại task hôm nay vào database
            userDataAccess.logTodayTask(todayTask.map { it.id })
            // Lưu vào cache
            val date = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val tasks = todayTask.map { it.id }
            cache.set(CachedKeys.SV_TODAY_TASK, Json.encodeToString(CachedTask(date, tasks)))
        } catch (e: Exception) {
            _logger.error("Error save task to cache")
        }
    }


    override fun getTodayTask(): List<DailyTask> {
        if (!::todayTask.isInitialized || todayTask.isEmpty()) {
            initialize()
        }
        return todayTask
    }

    private fun isSameDate(cachedDate: String): Boolean {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val parsedDate = LocalDate.parse(cachedDate, formatter)
        val currentDate = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate()
        return parsedDate.isEqual(currentDate)
    }

    private fun parseReward(reward: String): List<RewardDailyTask> {
        return try {
            reward.split("],[").map {
                val cleaned = it.replace("[", "").replace("]", "")
                val parts = cleaned.split(":")
                val itemId = parts[0].toInt()
                val amount = if (parts.size > 1) parts[1].toInt() else 1
                RewardDailyTask(itemId, amount)
            }
        } catch (e: Exception) {
            _logger.error("Error parsing reward: $reward")
            emptyList()
        }
    }


    // Call by admin command
    override fun hotReloadTodayTask(taskIds: List<Int>) {
        //Random 5 task mới
        if (taskIds.isEmpty()) {
            changeTask()
            _logger.log("Admin command reload daily success random")
            return
        }

        // tạo task theo danh sách của admin command
        val exitedTasks = configTask.filter { !it.isDeleted }

        val todayTask = exitedTasks.filter { taskIds.contains(it.id) }
        if (todayTask.size < numberTaskInDay) {
            throw Exception("Some task is deleted or not in config")
        }
        this.todayTask = todayTask
        this.todayTask = this.todayTask.sortedBy { it.id }

        _logger.log("Admin command reload daily success ids: $taskIds")

        // Lưu lại task hôm nay vào cache và log
        saveToCacheAndLog()
    }

    override fun hotReloadConfigTask() {
        loadConfig()
        _logger.log("Admin command reload config task success")
    }
}

