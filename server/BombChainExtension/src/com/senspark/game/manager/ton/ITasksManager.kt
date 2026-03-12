package com.senspark.game.manager.ton

import com.senspark.common.service.IServerService
import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.config.TonTasksConfig
import com.smartfoxserver.v2.entities.data.SFSArray

interface ITasksManager : IServerService {
    fun setConfig(config: MutableMap<Int, TonTasksConfig>)
    fun getUserTasks(userController: IUserController): SFSArray
    fun completeTask(userController: IUserController, taskId: Int): Boolean
    fun claimTaskReward(userController: IUserController, taskId: Int)
}