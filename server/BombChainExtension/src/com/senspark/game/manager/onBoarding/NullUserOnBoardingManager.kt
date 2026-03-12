package com.senspark.game.manager.onBoarding

import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class NullUserOnBoardingManager : IUserOnBoardingManager {
    override fun getConfig(): Map<Int, Float> {
        return emptyMap()
    }

    override fun getUserProgress(userId: Int): Int {
        return 0
    }

    override fun updateUserProgress(userProgress: UserProgress) {
    }
}