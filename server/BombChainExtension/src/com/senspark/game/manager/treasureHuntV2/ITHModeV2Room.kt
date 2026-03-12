package com.senspark.game.manager.treasureHuntV2

import com.senspark.game.controller.IUserController
import com.smartfoxserver.v2.entities.data.ISFSArray

interface ITHModeV2Room {
    fun joinRoom(controller: IUserController)
    fun leaveRoom(controller: IUserController)
    fun poolIdToKey(poolId: Int): String
    fun updateRewardPoolVariable(remainingPool: ISFSArray)
    fun updateConfigVariable(period: Int, maxPool: ISFSArray)
    fun updateTimeRefillPoolVariable(time: Long)
}

