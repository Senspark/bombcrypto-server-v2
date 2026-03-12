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
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val K_USER_NAME = "name"
private val MAX_LOGGED_OUT_TIME = 5.minutes.inWholeSeconds
private const val MAX_QUEUE = 5
private val KEEP_ALIVE_TIMEOUT = 15.seconds.inWholeSeconds // 15 seconds timeout for keep-alive

class UsersManager(logger: ILogger) : IUsersManager {
    private val _usersNames: ConcurrentHashMap<String, IUserController> = ConcurrentHashMap()
    private val _usersIds: ConcurrentHashMap<Int, ConcurrentHashMap<EnumConstants.DataType, IUserController>> = ConcurrentHashMap()
    private val _loggedOutUsers: ConcurrentHashMap<Int, ConcurrentHashMap<EnumConstants.DataType, Instant>> = ConcurrentHashMap()
    private val _initQueue: Queue<IUserController> = LinkedList()
    private val _scheduler: IScheduler = SmartFoxScheduler(1, logger)
    private val _logger = logger

    private val _checkAlive = CheckUserAlive(logger, KEEP_ALIVE_TIMEOUT)
    
    // Simple list to track UIDs that have client logging enabled
    private val _clientLoggingEnabledUids: MutableList<Int> = mutableListOf()

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

    override fun isClientLoggingEnabled(uid: Int): Boolean {
        return _clientLoggingEnabledUids.contains(uid)
    }

    override fun getAllUserControllersOfUid(uid: Int): List<IUserController> {
        return _usersIds[uid]?.values?.toList() ?: emptyList()
    }

    override fun createUserController(
        extension: SFSExtension,
        services: GlobalServices,
        user: User,
        userInfo: IUserInfo,
        factory: (userInfo: IUserInfo) -> IUserController,
        onCompleted: (userController: IUserController?) -> Unit
    ) {
        val userName = userInfo.username

        if (userName.isEmpty()) {
            extension.api.disconnectUser(user, KickReason.USER_NAME_IS_EMPTY)
            onCompleted(null)
            return
        }
        val userController: IUserController

        if (_usersNames.containsKey(userName)) {
            userController = _usersNames[userName]!!
            userController.setUserInfo(userInfo)
        } else {
            userController = factory(userInfo)
            userController.setUserInfo(userInfo)
        }
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
        
        // Remove from logged out users for this specific dataType
        _loggedOutUsers[userId]?.remove(dataType)
        // If no more data types for this user in logged out, remove the entire entry
        if (_loggedOutUsers[userId]?.isEmpty() == true) {
            _loggedOutUsers.remove(userId)
        }

        // Chỉ mới áp dụng cho sol, nào ton build lại client mới có gửi ping pong request thì check cái này
        if(userInfo.dataType == EnumConstants.DataType.SOL)
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
        return _loggedOutUsers.containsKey(userId) && _loggedOutUsers[userId]?.containsKey(dataType) == true
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
        return _usersNames.containsKey(userController.userName)
    }

    override fun updateKeepAliveTime(userId: Int, dataType: EnumConstants.DataType) {
        _checkAlive.updateKeepAliveTime(userId, dataType)
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

    private fun doJob() {
        clearLoggedOutUsers()
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
                    _usersNames.remove(controller.userName)
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

    private fun clearLoggedOutUsers() {
        val now = Instant.now()
        val removalList = mutableListOf<Pair<Int, EnumConstants.DataType>>()
        
        _loggedOutUsers.forEach { (userId, dataTypeMap) ->
            dataTypeMap.forEach { (dataType, lastLogout) ->
                if (now.minusSeconds(MAX_LOGGED_OUT_TIME) > lastLogout) {
                    removalList.add(Pair(userId, dataType))
                }
            }
        }
        
        removalList.forEach { (userId, dataType) ->
            val userController = getUserController(userId, dataType)
            if (userController != null) {
                disposeUser(userController)
            }
        }
    }

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
            _checkAlive.removeTimeout(userId, dataType)
        }
        _loggedOutUsers.remove(userId)
        _logger.log("Dispose user ${userController.userName}")
    }
}