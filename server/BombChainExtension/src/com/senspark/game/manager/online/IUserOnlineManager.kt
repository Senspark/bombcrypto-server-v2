package com.senspark.game.manager.online

import com.senspark.common.service.IGlobalService
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.EnumConstants.DeviceType
import com.senspark.game.declare.EnumConstants.Platform

/**
 * Manager for tracking online users in Redis
 */
interface IUserOnlineManager : IGlobalService {
    /**
     * Track user as online in Redis
     */
    fun trackUserOnline(userId: Int, userName: String, dataType: DataType?, deviceType: DeviceType, platform: Platform)

    /**
     * Remove user from online tracking in Redis
     */
    fun removeUserOnline(userId: Int)

    /**
     * Check if a user is currently online
     */
    fun isUserExitsOnRedis(userId: Int): Boolean

    /**
     * Get all online users
     */
    fun getAllUserOnlineFromRedis(): Map<String, String?>
}
