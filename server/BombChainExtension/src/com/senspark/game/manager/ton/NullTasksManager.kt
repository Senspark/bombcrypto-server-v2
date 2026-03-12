package com.senspark.game.manager.ton

import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.config.TonTasksConfig
import com.smartfoxserver.v2.entities.data.SFSArray

class NullTasksManager : ITasksManager {
    override fun initialize() {
    }

    override fun setConfig(config: MutableMap<Int, TonTasksConfig>) {}

    override fun getUserTasks(userController: IUserController): SFSArray {
        return SFSArray()
    }

    override fun completeTask(userController: IUserController, taskId: Int): Boolean {
        return true
    }

    override fun claimTaskReward(userController: IUserController, taskId: Int) {}
}