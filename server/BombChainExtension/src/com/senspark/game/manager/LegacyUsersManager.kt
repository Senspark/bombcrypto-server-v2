package com.senspark.game.manager

import com.senspark.common.service.IScheduler
import com.senspark.common.utils.ILogger
import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.KickReason
import com.senspark.game.extension.GlobalServices
import com.senspark.game.manager.user.CheckUserAlive
import com.senspark.game.utils.SmartFoxScheduler
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.extensions.SFSExtension
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

private const val K_USER_NAME = "name"
private const val MAX_QUEUE = 5
private val KEEP_ALIVE_TIMEOUT = 15.seconds.inWholeSeconds // 15 seconds timeout for keep-alive

class LegacyUsersManager(logger: ILogger) : IUsersManager {
    private val _usersNames: ConcurrentHashMap<String, IUserController> = ConcurrentHashMap()
    private val _usersIds: ConcurrentHashMap<Int, ConcurrentHashMap<EnumConstants.DataType, IUserController>> = ConcurrentHashMap()
    private val _initQueue: Queue<IUserController> = LinkedList()
    private val _scheduler: IScheduler = SmartFoxScheduler(1, logger)
    private val _logger = logger
    
    // Simple list to track UIDs that have client logging enabled
    private val _clientLoggingEnabledUids: MutableList<Int> = mutableListOf()
    
    private val _checkAlive = CheckUserAlive(logger, KEEP_ALIVE_TIMEOUT)

    override fun initialize() {
        _scheduler.schedule(
            "UsersManager",
            0,
            1000,
            ::doJob,
        )
    }

    override fun dispose() {
        _usersNames.forEach { (_, v) ->
            v.disconnect(KickReason.UNKNOWN)
        }
    }

    override fun createUserController(
        extension: SFSExtension,
        services: GlobalServices,
        user: User,
        userInfo: IUserInfo,
        factory: (userInfo: IUserInfo)-> IUserController,
        onCompleted: (userController: IUserController?) -> Unit
    ) {
        val userName = userInfo.username

        if (userName.isEmpty()) {
            extension.api.disconnectUser(user, KickReason.USER_NAME_IS_EMPTY)
            onCompleted(null)
            return
        }

        val userController = factory(userInfo)
        val initSuccess = userController.verifyAndUpdateUserHash()
        if (!initSuccess) {
            extension.api.disconnectUser(user, KickReason.NEED_LOGIN_AGAIN)
            onCompleted(null)
            return
        }

        user.setProperty(K_USER_NAME, userName)
        _usersNames[userName] = userController
        
        // Store in nested map structure: userId -> dataType -> userController
        val userId = userInfo.id
        val dataType = userInfo.dataType
        if (!_usersIds.containsKey(userId)) {
            _usersIds[userId] = ConcurrentHashMap()
        }
        _usersIds[userId]!![dataType] = userController
        
        _checkAlive.addUserToCheck(userInfo.id, userInfo.dataType)

        userController.setUser(user)
        _initQueue.add(userController)

        onCompleted(userController)
    }

    override fun remove(userName: String) {
        if (_usersNames.containsKey(userName)) {
            disposeUser(_usersNames[userName]!!)
        }
    }

    override fun remove(userController: IUserController) {
        remove(userController.userName)
    }

    override fun kickAndRemoveUser(userId: Int) {
        val controller = getUserController(userId)
        if (controller != null) {
            kickAndRemoveUser(controller)
        }
    }

    override fun kickAndRemoveUser(userName: String) {
        val controller = getUserController(userName)
        if (controller != null) {
            kickAndRemoveUser(controller)
        }
    }

    override fun isUserLoggedOut(userId: Int, dataType: EnumConstants.DataType): Boolean {
        return true;
    }

    override fun isLoggedIn(userId: Int, dataType: EnumConstants.DataType): Boolean {
        return _usersIds.containsKey(userId) && _usersIds[userId]?.containsKey(dataType) == true
    }

    private fun kickAndRemoveUser(userController: IUserController) {
        userController.disconnect(KickReason.KICK)
        disposeUser(userController)
    }

    override fun getUserId(userName: String): Int {
        return _usersNames[userName]?.userId ?: -1
    }

    override fun getUserController(userName: String): IUserController? {
        return _usersNames[userName]
    }

    override fun getUserController(user: User): IUserController? {
        val userName = user.getProperty(K_USER_NAME) as String? ?: return null
        return _usersNames[userName]
    }

    // Legacy method - returns the first available controller or null
    override fun getUserController(userId: Int): IUserController? {
        val userControllerMap = _usersIds[userId]
        return userControllerMap?.values?.firstOrNull()
    }
    
    // New method with DataType - should be used going forward
    override fun getUserController(userId: Int, dataType: EnumConstants.DataType): IUserController? {
        return _usersIds[userId]?.get(dataType)
    }

    override fun checkExistence(userController: IUserController): Boolean {
        return _usersNames.containsKey(userController.userName.lowercase())
    }
    
    // Method to update keep-alive time for a user
    override fun updateKeepAliveTime(userId: Int, dataType: EnumConstants.DataType) {
        _checkAlive.updateKeepAliveTime(userId, dataType)
    }

    private fun doJob() {
        initUserControllers()
        _checkAlive.checkKeepAlive()
    }
    

    private fun initUserControllers() {
        var size = _initQueue.size
        if (size > MAX_QUEUE) {
            size = MAX_QUEUE
        }
        for (i in 0 until size) {
            if (_initQueue.isEmpty()) {
                return
            }
            val controller = _initQueue.poll()
            if (controller != null) {
                val success = controller.initDependencies()
                if (!success) {
                    _usersNames.remove(controller.userName.lowercase())
                    val userId = controller.userId
                    val userInfo = controller.userInfo
                    val dataType = userInfo?.dataType
                    if (dataType != null) {
                        _usersIds[userId]?.remove(dataType)
                        // If no more data types for this user, remove the entire entry
                        if (_usersIds[userId]?.isEmpty() == true) {
                            _usersIds.remove(userId)
                        }
                        _checkAlive.removeKeepAlive(userId, dataType)
                    } else {
                        // Fallback: remove all entries for this user
                        _usersIds.remove(userId)
                    }
                    controller.disconnect(KickReason.NEED_LOGIN_AGAIN)
                }
            }
        }
    }
    
    // Kiểm tra session cũ có còn ko để clear đi cho session mới connect vào
    override fun safeCheckAndDisposeOldSession(userName: String){
        val uid = getUserId(userName)
        if(uid == -1){
            return
        }
        val userController = getUserController(userName)
        if(userController != null) {
            val userInfo = userController.userInfo
            val dataType = userInfo?.dataType
            if(dataType != null && _checkAlive.isHaveOldSession(uid, dataType)){
                disposeUser(userController)
            }
        }
    }

    /**
     * Toggles client logging for a specific user ID
     * @param uid User ID to enable/disable logging for
     * @param sendLog true to enable logging, false to disable it
     */
    override fun setClientLogging(uid: Int, sendLog: Boolean) {
        if (sendLog) {
            if (!_clientLoggingEnabledUids.contains(uid)) {
                _clientLoggingEnabledUids.add(uid)
                _logger.log("Enabled client logging for user ID: $uid")
            }
        } else {
            _clientLoggingEnabledUids.remove(uid)
            _logger.log("Disabled client logging for user ID: $uid")
        }
    }

    /**
     * Checks if client logging is enabled for a specific user ID
     * @param uid User ID to check
     * @return true if logging is enabled for the user, false otherwise
     */
    override fun isClientLoggingEnabled(uid: Int): Boolean {
        return _clientLoggingEnabledUids.contains(uid)
    }

    override fun getAllUserControllersOfUid(uid: Int): List<IUserController> {
        return _usersIds[uid]?.values?.toList() ?: emptyList()
    }

    // When a user is disposed, we should also remove them from the logging list
    private fun disposeUser(userController: IUserController) {
        userController.dispose()
        _usersNames.remove(userController.userName)
        val userId = userController.userId
        
        // Get the data type from the user info to remove the specific entry
        val userInfo = userController.userInfo
        val dataType = userInfo?.dataType
        if (dataType != null) {
            _usersIds[userId]?.remove(dataType)
            // If no more data types for this user, remove the entire entry
            if (_usersIds[userId]?.isEmpty() == true) {
                _usersIds.remove(userId)
            }
        } else {
            // Fallback: remove all entries for this user
            _usersIds.remove(userId)
        }
        
        if (dataType != null) {
            _checkAlive.removeKeepAlive(userId, dataType)
            _checkAlive.removeTimeout(userId, dataType)
        }
        _logger.log("Dispose user ${userController.userName}")
    }
}
