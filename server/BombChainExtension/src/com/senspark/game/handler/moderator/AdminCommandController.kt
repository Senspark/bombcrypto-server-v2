package com.senspark.game.handler.moderator

import com.senspark.common.cache.ICacheService
import com.senspark.common.utils.IGlobalLogger
import com.senspark.game.constant.CachedKeys
import com.senspark.game.data.manager.autoMine.IAutoMineManager
import com.senspark.game.data.manager.block.IBlockRewardDataManager
import com.senspark.game.data.manager.pvp.IPvpConfigManager
import com.senspark.game.data.manager.treassureHunt.IHouseManager
import com.senspark.game.data.manager.treassureHunt.ITreasureHuntConfigManager
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.extension.GlobalServices
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.extension.modules.ServerType
import com.senspark.game.manager.IUsersManager
import com.senspark.game.manager.dailyTask.IDailyTaskManager
import com.senspark.game.manager.rock.IUserRockManager
import com.senspark.game.manager.stake.IHeroStakeManager
import com.senspark.game.manager.ton.IReferralManager
import com.senspark.game.manager.ton.ITasksManager
import com.senspark.game.manager.treasureHuntV2.ITreasureHuntV2Manager
import com.senspark.game.pvp.HandlerCommand
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.Zone
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import com.smartfoxserver.v2.extensions.SFSExtension
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

class AdminCommandController(
    private val _services: GlobalServices,
) {
    private val _dataAccessManager = _services.get<IDataAccessManager>()
    private val _treasureHuntDataManager = _services.get<ITreasureHuntConfigManager>()
    private val _blockRewardDataManager = _services.get<IBlockRewardDataManager>()
    private val _netServices = _services.get<ISvServicesContainer>()
    private val _autoMineManager = _services.get<IAutoMineManager>()
    private val _gameConfigManager = _services.get<IGameConfigManager>()
    private val _logger = _services.get<IGlobalLogger>()
    private val _redis = _services.get<ICacheService>();
    
    // Local list of logged users
    private val _loggedUsers = mutableListOf<LoggedUser>()

    private lateinit var _zone: Zone
    private lateinit var _extension: SFSExtension

    fun init(zone: Zone, extension: SFSExtension) {
        _zone = zone
        _extension = extension
        // Khi khởi đông server thì chỉ cần xoá hết trên redis và ko log user nào cả
        clearLoggedUsersFromRedis()
        // hiện xoá hết chứ ko cần load từ redis lại, để đây khi cần support vẫn log các user khi server restart
        //loadLoggedUsersFromRedis()
    }
    
    /**
     * Clear the logged users list from Redis
     */
    private fun clearLoggedUsersFromRedis() {
        try {
            _redis.set(CachedKeys.SV_CURRENT_USER_SEND_LOG, "")
            synchronized(_loggedUsers) {
                _loggedUsers.clear()
            }
            _logger.log("Cleared logged users list from Redis")
        } catch (e: Exception) {
            _logger.error("Failed to clear logged users from Redis: ${e.message}")
        }
    }
    
    /**
     * Load the current list of logged users from Redis
     */
    private fun loadLoggedUsersFromRedis() {
        try {
            val loggedUsersJson = _redis.get(CachedKeys.SV_CURRENT_USER_SEND_LOG)
            if (!loggedUsersJson.isNullOrEmpty()) {
                val loggedUsersList = Json.decodeFromString<LoggedUserList>(loggedUsersJson)
                synchronized(_loggedUsers) {
                    _loggedUsers.clear()
                    _loggedUsers.addAll(loggedUsersList.users)
                }
                _logger.log("Loaded ${_loggedUsers.size} logged users from Redis")
            }
        } catch (e: Exception) {
            _logger.error("Failed to load logged users from Redis: ${e.message}")
        }
    }
    
    /**
     * Save the current list of logged users to Redis
     */
    private fun saveLoggedUsersToRedis() {
        try {
            val loggedUsersList = LoggedUserList(_loggedUsers.toMutableList())
            val json = Json.encodeToString(loggedUsersList)
            _redis.set(CachedKeys.SV_CURRENT_USER_SEND_LOG, json)
            _logger.log("Saved ${_loggedUsers.size} logged users to Redis")
        } catch (e: Exception) {
            _logger.error("Failed to save logged users to Redis: ${e.message}")
        }
    }

    /**
     * table: config_block_reward
     */
    fun hotReloadConfigBlockReward() {
        _blockRewardDataManager.setConfig(_dataAccessManager.shopDataAccess.loadBlockReward())
    }

    /**
     * table: config_min_stake_hero
     */
    fun hotReloadConfigMinStakeHero() {
        val heroesStakeManager = _netServices.get(ServerType.BNB_POL).get<IHeroStakeManager>()
        heroesStakeManager.setConfig(_dataAccessManager.shopDataAccess.loadMinStakeHeroConfig())
    }

    fun hotReloadConfigPackageAutoMine() {
        _autoMineManager.setConfig(_dataAccessManager.shopDataAccess.loadAutoMinePackageConfig())
    }

    fun setStopPoolTHModeV2() {
        val treasureHuntV2Manager = _netServices.get(ServerType.BNB_POL).get<ITreasureHuntV2Manager>()
        treasureHuntV2Manager.setStopPool(true)
    }

    fun hotReloadConfigTHModeV2() {
        val treasureHuntV2Manager = _netServices.get(ServerType.BNB_POL).get<ITreasureHuntV2Manager>()
        treasureHuntV2Manager.setStopPool(true)
        treasureHuntV2Manager.reloadConfigs()
        treasureHuntV2Manager.setStopPool(false)
    }

    fun hotReloadTreasureHuntDataConfig() {
        val treasureHuntDataAccess = _dataAccessManager.thModeDataAccess
        _treasureHuntDataManager.setDataConfig(treasureHuntDataAccess.loadTreasureHuntDataConfig())
    }

    fun hotReloadBurnHeroConfig() {
        val shopDataAccess = _dataAccessManager.shopDataAccess
        val userRockManager = _netServices.get(ServerType.BNB_POL).get<IUserRockManager>()
        userRockManager.setConfig(shopDataAccess.loadBurnHeroConfig())
    }

    fun dumpConfigBlockReward(): String {
        return _blockRewardDataManager.dumpRewards()
    }

    fun hotReloadPvpConfig() {
        val pvpConfigManager = _netServices.get(ServerType.BNB_POL).get<IPvpConfigManager>()
        pvpConfigManager.reloadConfig()
    }

    fun hotReloadTonTasks() {
        val thModeDataAccess = _dataAccessManager.thModeDataAccess
        val tonTasksManager = _netServices.get(ServerType.TON).get<ITasksManager>()
        tonTasksManager.setConfig(thModeDataAccess.getTasksConfig())
    }

    fun kickUser(params: ISFSObject) {
        val key = "userName"
        if (!params.containsKey(key)) {
            throw Exception("Missing user name")
        }
        val userName = params.getUtfString(key)
        kickUser(userName)
    }

    fun kickUser(userName: String) {
        _netServices.filter(IUsersManager::class).forEach {
            it.kickAndRemoveUser(userName)
        }
    }

    fun hotReloadReferralParams() {
        val referralManager = _netServices.get(ServerType.TON).get<IReferralManager>()
        referralManager.setConfig(
            _dataAccessManager.thModeDataAccess.getReferralParamsConfig()
        )
    }

    fun hotReloadConfigPackageHouseRent() {
        val shopDataAccess = _dataAccessManager.shopDataAccess
        _netServices.get(ServerType.TON).get<IHouseManager>().setConfig(shopDataAccess.loadHouseRentPackageConfig())
    }
    
    fun hotReloadDailyTask(taskIds: List<Int>) {
        val dailyTaskManager = _netServices.get(ServerType.BNB_POL).get<IDailyTaskManager>()
        
        if(taskIds.isEmpty()) {
            dailyTaskManager.hotReloadTodayTask(taskIds)
            return  
        }
        
//        if(!taskIds.containsAll(listOf(1,2,3))) {
//            throw Exception("Task must contain default task ids 1,2,3")
//        }
        if(taskIds.size != _gameConfigManager.totalTaskInDay) {
            throw Exception("Task must equal to ${_gameConfigManager.totalTaskInDay} task")
        }
        dailyTaskManager.hotReloadTodayTask(taskIds)
    }

    fun hotReloadDailyTaskConfig() {
        val dailyTaskManager = _netServices.get(ServerType.BNB_POL).get<IDailyTaskManager>()
        dailyTaskManager.hotReloadConfigTask()
    }
//{ "cmd": "force_client_send_log", "data": "{\"uid\":111,\"type\":\"BNB_POL\",\"send_log\":true}" }
    fun forceClientSendLog(data: String?) {
        try {
            if (data.isNullOrEmpty()) {
                _logger.error("No data provided for client log request.")
                return
            }
            val logData = Json.decodeFromString<ClientLogRequest>(data)
            _logger.log("Received log request for user ${logData.uid}, type: ${logData.type}, send_log: ${logData.send_log}")
            
            // Update client logging status for the user with network type filter
            // Parse the type from request to ServerType, default to BNB_POL if empty or invalid
            val serverType = try {
                if (logData.type.isNotBlank()) {
                    ServerType.valueOf(logData.type.uppercase())
                } else {
                    ServerType.BNB_POL
                }
            } catch (e: IllegalArgumentException) {
                _logger.warn("Invalid server type: ${logData.type}, using BNB_POL as default")
                ServerType.BNB_POL
            }
            
            val userManager = _netServices.get(serverType).get<IUsersManager>()
            userManager.setClientLogging(logData.uid, logData.send_log)
            
            // Get the user's controller to extract username if available
            val userController = userManager.getUserController(logData.uid)
            val userName = userController?.userName
            
            // Update the list of users being logged
            updateLoggedUsersList(logData.uid, userName, serverType.name, logData.send_log)
            
            if(logData.send_log && userController != null) {
                userController.sendDataEncryption(HandlerCommand.ForceClientSendLog, SFSObject())
            }
            
            _logger.log("Client logging ${if (logData.send_log) "enabled" else "disabled"} for user ID: ${logData.uid} on network type: ${serverType}")
        } catch (e: Exception) {
            _logger.error("Error processing client log request: ${e.message}")
        }
    }
    
    /**
     * Updates the list of users being logged in Redis
     * @param uid User ID
     * @param userName User name (if available)
     * @param networkType Network type (e.g., BNB_POL)
     * @param isLogging Whether to add (true) or remove (false) the user from the list
     */
    private fun updateLoggedUsersList(uid: Int, userName: String?, networkType: String, isLogging: Boolean) {
        try {
            synchronized(_loggedUsers) {
                if (isLogging) {
                    // Add or update user in the local list
                    val existingUserIndex = _loggedUsers.indexOfFirst { it.uid == uid && it.networkType == networkType }
                    
                    if (existingUserIndex >= 0) {
                        // Update existing user
                        _loggedUsers[existingUserIndex] = LoggedUser(
                            uid = uid,
                            userName = userName,
                            networkType = networkType,
                            timeUpdated = System.currentTimeMillis()
                        )
                    } else {
                        // Add new user
                        _loggedUsers.add(
                            LoggedUser(
                                uid = uid,
                                userName = userName,
                                networkType = networkType
                            )
                        )
                    }
                    _logger.log("Added/updated user $uid ($userName) to logged users list")
                } else {
                    // Remove user from the local list
                    val initialSize = _loggedUsers.size
                    _loggedUsers.removeIf { it.uid == uid && it.networkType == networkType }
                    
                    if (initialSize != _loggedUsers.size) {
                        _logger.log("Removed user $uid from logged users list")
                    }
                }
            }
            // Save the updated list to Redis
            saveLoggedUsersToRedis()
        } catch (e: Exception) {
            _logger.error("Error updating logged users list: ${e.message}")
        }
    }
}

@Serializable
data class ClientLogRequest(
    val uid: Int,
    val type: String,
    val send_log: Boolean = false,
)

@Serializable
data class LoggedUser(
    val uid: Int,
    val userName: String?,
    val networkType: String,
    val timeUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class LoggedUserList(
    val users: MutableList<LoggedUser> = mutableListOf()
)
