package com.senspark.game.manager.treasureHuntV2

import com.senspark.game.controller.IUserController
import com.smartfoxserver.v2.entities.data.ISFSArray

class NullTHModeV2Room : ITHModeV2Room {
    override fun joinRoom(controller: IUserController) {
    }

    override fun leaveRoom(controller: IUserController) {
    }

    override fun poolIdToKey(poolId: Int): String {
        return ""
    }

    override fun updateRewardPoolVariable(remainingPool: ISFSArray) {
    }

    override fun updateConfigVariable(period: Int, maxPool: ISFSArray) {
    }

    override fun updateTimeRefillPoolVariable(time: Long) {
    }
}