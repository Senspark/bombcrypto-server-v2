package com.senspark.game.manager.online

import com.senspark.common.cache.ICacheService
import com.senspark.common.utils.IGlobalLogger
import com.senspark.game.constant.CachedKeys
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.EnumConstants.DeviceType
import com.senspark.game.declare.EnumConstants.Platform
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import kotlin.time.Duration.Companion.days

/**
 * Implementation of IUserOnlineManager to track online users in Redis
 */
class UserOnlineManager(
    private val cacheService: ICacheService,
    private val logger: IGlobalLogger
) : IUserOnlineManager {

    /**
     * Track user as online in Redis
     */
    override fun trackUserOnline(
        userId: Int,
        userName: String,
        dataType: DataType?,
        deviceType: DeviceType,
        platform: Platform
    ) {
        try {
            val userOnlineInfo = buildJsonObject {
                put("user_name", userName)
                put("login_time", Instant.now().toString())
                put("data_type", dataType?.toString() ?: "")
                put("device_type", deviceType.toString())
                put("platform", platform.toString())
            }

            cacheService.setToHash(CachedKeys.USER_ONLINE, userId.toString(), userOnlineInfo.toString(), 1.days)
            logger.log("User online status stored in Redis: $userId")
        } catch (ex: Exception) {
            logger.error("Failed to store user online status in Redis: ${ex.message}", ex)
        }
    }

    /**
     * Remove user from online tracking in Redis
     */
    override fun removeUserOnline(userId: Int) {
        try {
            cacheService.deleteFromHash(CachedKeys.USER_ONLINE, userId.toString())
            logger.log("User removed from online tracking: $userId")
        } catch (ex: Exception) {
            logger.error("Failed to remove user from Redis: ${ex.message}", ex)
        }
    }

    /**
     * Check if a user is currently online
     */
    override fun isUserExitsOnRedis(userId: Int): Boolean {
        return try {
            cacheService.getFromHash(CachedKeys.USER_ONLINE, userId.toString()) != null
        } catch (ex: Exception) {
            logger.error("Error checking if user is online: ${ex.message}", ex)
            false
        }
    }

    /**
     * Get all online users
     */
    override fun getAllUserOnlineFromRedis(): Map<String, String?> {
        return try {
            cacheService.getAllFromHash(CachedKeys.USER_ONLINE)
        } catch (ex: Exception) {
            logger.error("Error getting all online users: ${ex.message}", ex)
            emptyMap()
        }
    }

    override fun initialize() {
    }
}
