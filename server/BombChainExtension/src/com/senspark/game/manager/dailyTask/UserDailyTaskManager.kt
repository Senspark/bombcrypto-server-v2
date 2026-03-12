package com.senspark.game.manager.dailyTask

import com.senspark.common.utils.map
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.model.config.IGachaChestItem
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.customEnum.GachaChestType
import com.senspark.game.manager.dailyTask.UserTask.Companion.parseRewardForClaim
import com.senspark.game.user.IGachaChestManager
import com.senspark.lib.data.manager.IGameConfigManager
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

class UserDailyTaskManager(
    private val _mediator: UserControllerMediator,
) : IUserDailyTaskManager {
    private val userDataAccess = _mediator.services.get<IUserDataAccess>()
    private val gameConfigManager = _mediator.services.get<IGameConfigManager>()
    private val gachaChestManager = _mediator.svServices.get<IGachaChestManager>()
    private val dailyTaskManager = _mediator.svServices.get<IDailyTaskManager>()

    private lateinit var todayUserTask: TodayTask
    private lateinit var toDayUserTaskId: List<Int>
    private lateinit var toDayConfigTaskList: List<DailyTask>
    private lateinit var configUrl: String
    private var today: LocalDate = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate()

    private val dailyChest = gachaChestManager.getChest(GachaChestType.DAILY)

    // Này chỉ gọi 1 lần lúc vào game hoặc khi qua ngày mới client phải gọi lại để lấy task mới
    // vậy nên mỗi lần gọi phải vào db để kt xem có qua ngày chưa và gọi lại config để xem có thay đổi gì trong config ko
    override fun getUserTodayTask(): TodayTask {
        today = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate()
        configUrl = gameConfigManager.dailyTaskConfigUrl
        toDayConfigTaskList = dailyTaskManager.getTodayTask()

        val result = userDataAccess.getUserDailyTask(_mediator.userId)
        val tasksArray = result.getSFSArray("tasks")
        val taskList = tasksArray?.map {
            UserTask(
                taskId = it.getInt("task_id"),
                progress = it.getInt("progress"),
                claimed = it.getBool("claimed"),
                reward = parseRewardForClaim(it.getUtfString("reward"))
            )
        }
        // Hôm nay user này mới vào game, tạo task với giá trị mặc định
            ?: toDayConfigTaskList.map {
                UserTask(
                    taskId = it.id, progress = 0, claimed = false, reward = createRewardForClaim(it.id) ?: emptyList()
                )
            }

        todayUserTask = TodayTask(
            finalRewardClaimed = result.getBool("final_reward_claimed") ?: false,
            tasks = taskList,
            urlConfig = configUrl
        )

        toDayUserTaskId = todayUserTask.tasks.map { it.taskId }

        // Dev ko reload lại 5 task bằng admin command thì ko cần check cái này
        checkWithConfig()

        return todayUserTask
    }

    override fun getUserDailyProgress(): List<Int> {
        if(!::toDayUserTaskId.isInitialized || toDayUserTaskId.isEmpty()) {
            _mediator.logger.log("User ${_mediator.userId} task list is not initialized")
            return emptyList()
        }
        return todayUserTask.tasks.map { it.progress }
    }

    // Đảm bảo task hôm nay của user khớp vói các task trong config
    // các task ko đúng, dư thiếu sẽ đc điều chỉnh ở đây
    private fun checkWithConfig() {
        val configTaskIds = toDayConfigTaskList.map { it.id }.toSet()
        val userTaskIds = toDayUserTaskId.toSet()

        // Tìm các task user có mà trên connfig đã bỏ
        val invalidTaskIds = userTaskIds.filterNot { configTaskIds.contains(it) }
        // Tìm các task đã đc làm mới mà user chưa có
        val missingTaskIds = configTaskIds.filterNot { userTaskIds.contains(it) }

        // Các task đều hợp lệ, ko dư, thiếu hay sai task nào
        if (invalidTaskIds.isEmpty() && missingTaskIds.isEmpty()) {
            return
        }

        if (invalidTaskIds.isNotEmpty()) {
            // Loại bỏ các task cũ ko hợp lệ của user
            todayUserTask.tasks = todayUserTask.tasks.filterNot { task -> task.taskId in invalidTaskIds }
        }

        if (missingTaskIds.isNotEmpty()) {
            // Thêm các task mới đc reload trên config vào task của user
            val newTasks = toDayConfigTaskList.filter { it.id in missingTaskIds }.map {
                UserTask(
                    taskId = it.id, progress = 0, claimed = false, reward = createRewardForClaim(it.id) ?: emptyList()
                )
            }
            todayUserTask.tasks += newTasks
        }

        todayUserTask.tasks = todayUserTask.tasks.sortedBy { it.taskId }
        toDayUserTaskId = todayUserTask.tasks.map { it.taskId }

        saveToDatabase()
        _mediator.logger.log("User ${_mediator.userId} task list was not matching with config and has been updated")
    }


    // Server tự check và update
    override fun updateProgressTask(taskIds: List<Int>, amount: Int) {
        if (amount < 0) {
            _mediator.logger.log("Update task user ${_mediator.userId} fail, amount must be greater than 0")
            return
        }
        // Ko cần update và log
        if (amount == 0) {
            return
        }
        taskIds.forEach {
            updateProgressTask(it, amount)
        }

        // Do có nhiều network có thể chơi daily task cùng lúc nên mỗi khi có update gì sẽ lưu db ngay
        saveToDatabase()
    }

    private fun updateProgressTask(taskId: Int, amount: Int) {
        val task = getTask(taskId) ?: // Task này hôm nay ko có
        return
        // Đã claim final reward tức là đã xong hết task, ko cần check nữa
        if (todayUserTask.finalRewardClaimed) {
            return
        }
        
        if (task.claimed) {
            return
        }
        if (isCompletedTask(task)) {
            return
        }
        task.progress += amount
    }

    override fun claimTaskReward(taskId: Int): List<ItemForClaim>? {
        //Do có nhiều network chơi daily cùng lúc nên giờ mỗi khi claim sẽ phải sync với database lại trước khi cho claim
        // Reload lại với database
        getUserTodayTask()

        if(!isSameDate()) {
            _mediator.logger.log("Claim task user ${_mediator.userId} fail, today reward is expired")
            return null
        }

        // Đã claim final reward tức là đã xong hết task, ko cần check nữa
        if (todayUserTask.finalRewardClaimed) {
            return null
        }

        val task = getTask(taskId)
        if (task == null) {
            _mediator.logger.error("User ${_mediator.userId}, task $taskId not found")
            return null
        }
        if (task.claimed) {
            _mediator.logger.log("Claim task user ${_mediator.userId} fail, task $taskId is already claimed")
            return null
        }
        if (!isCompletedTask(task)) {
            _mediator.logger.log("Claim task user ${_mediator.userId} fail, task $taskId is not completed")
            return null
        }

        task.claimed = true

        // Do có nhiều network có thể chơi daily task cùng lúc nên claim xong lưu database ngay
        saveToDatabase()

        return task.reward
    }

    private fun createRewardForClaim(taskId: Int): List<ItemForClaim>? {
        val rewardList = mutableListOf<ItemForClaim>()

        val config = toDayConfigTaskList.find { it.id == taskId }
        if (config == null) {
            _mediator.logger.error("Task $taskId not found in config")
            return null
        }
        if (config.isRandom) {
            val randomReward = config.reward.random()
            rewardList.add(ItemForClaim(randomReward.itemId, randomReward.quantity, config.expired))
        } else {
            rewardList.addAll(config.reward.map { ItemForClaim(it.itemId, it.quantity, config.expired) })
        }
        return rewardList
    }

    private fun getTask(taskId: Int): UserTask? {
        if (!::toDayUserTaskId.isInitialized) {
            toDayUserTaskId = emptyList()
            return null
        }
        if (!toDayUserTaskId.contains(taskId)) {
            return null
        }
        return todayUserTask.tasks.find { it.taskId == taskId }
    }

    private fun isCompletedTask(task: UserTask): Boolean {
        val currentProgress = task.progress
        val configProgress = toDayConfigTaskList.find { it.id == task.taskId }!!.completed
        return currentProgress >= configProgress
    }

    // Chỉ save khi user claim reward hoặc thoát game
    override fun saveToDatabase() {
        if (!::todayUserTask.isInitialized) {
            return
        }
        if(!isSameDate()) {
            _mediator.logger.log("New day, user ${_mediator.userId} task will not saved to database")
            return
        }
        userDataAccess.updateUserDailyTask(_mediator.userId, todayUserTask)
    }

    override fun claimFinalReward(): List<IGachaChestItem> {
        if (todayUserTask.finalRewardClaimed) {
            return emptyList()
        }

        if (todayUserTask.tasks.any { !it.claimed }) {
            _mediator.logger.log("Claim final reward user ${_mediator.userId} fail, not all task is claimed")
            return emptyList()
        }

        todayUserTask.finalRewardClaimed = true
        saveToDatabase()
        return dailyChest.randomItems()
    }
    
    private fun isSameDate(): Boolean {
        val currentDate = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate()
        return currentDate.isEqual(today)
    }
}
