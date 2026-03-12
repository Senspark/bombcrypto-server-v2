package com.senspark.game.manager.onBoarding

import com.smartfoxserver.v2.entities.data.ISFSObject

interface IUserOnBoardingManager {
    // Id step and number of star core reward.
    fun getConfig(): Map<Int, Float>
    fun getUserProgress(userId: Int): Int
    fun updateUserProgress(userProgress: UserProgress)
}