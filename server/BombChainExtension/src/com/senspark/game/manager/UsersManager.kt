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
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val K_USER_NAME = "name"
private val MAX_LOGGED_OUT_TIME = 5.minutes.inWholeSeconds
private const val MAX_QUEUE = 5
private const val SLOW_INIT_MS = 200L
// 90s thay vì 15s: tránh evict nhầm tab nền bị browser throttle timer JS (xem chú thích ở LegacyUsersManager).
private val KEEP_ALIVE_TIMEOUT = 90.seconds.inWholeSeconds

class UsersManager(logger: ILogger) : IUsersManager {
    private val _usersNames: ConcurrentHashMap<String, IUserController> = ConcurrentHashMap()
    private val _usersIds: ConcurrentHashMap<Int, ConcurrentHashMap<EnumConstants.DataType, IUserController>> = ConcurrentHashMap()
    private val _loggedOutUsers: ConcurrentHashMap<Int, ConcurrentHashMap<EnumConstants.DataType, Instant>> = ConcurrentHashMap()
    // Concurrent, not LinkedList: added on the join-zone thread, polled on this manager's scheduler thread.
    // See the same field in LegacyUsersManager.
    private val _initQueue: Queue<IUserController> = ConcurrentLinkedQueue()
    private val _scheduler: IScheduler = SmartFoxScheduler(1, logger)
    private val _logger = logger

    private val _checkAlive = CheckUserAlive(logger, KEEP_ALIVE_TIMEOUT)
    
    // Simple list to track UIDs that have client logging enabled
    private val _clientLoggingEnabledUids: MutableSet<Int> = ConcurrentHashMap.newKeySet()

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
        landing: EnumConstants.Landing,
        forceLogin: Boolean,
        factory: (userInfo: IUserInfo) -> IUserController,
        onCompleted: (userController: IUserController?) -> Unit
    ) {
        // Single-mode (TON/SOL/RON/BAS/VIC): ≤1 phiên/(uid,dataType), tên không rename #landing nên SFS-core vẫn
        // dedup theo tên như cũ. Giữ nguyên hành vi takeover-by-overwrite; force_login đã được xử lý best-effort
        // ở ForceLoginManager.checkToKickAccountFi (skip dialog) nên không cần admission riêng ở đây.
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
        // Các network dùng UsersManager là single-mode nên landing luôn WILDCARD; vẫn lưu để keep-alive nhất quán.
        userController.landing = landing
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
            _checkAlive.addUserToCheck(userInfo.id, userInfo.dataType, landing)


        userController.setUser(user)
        _initQueue.add(userController)
        _logger.log("[InitQueue] enqueue uid=$userId dt=$dataType landing=$landing queued=${_initQueue.size}")

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
        getAllUserControllersOfUid(userId).forEach { kickAndRemoveUser(it) }
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

    override fun hasLiveConflict(userId: Int, dataType: EnumConstants.DataType, landing: EnumConstants.Landing): Boolean {
        // Single-mode network: ≤1 phiên/(uid,dataType), bỏ qua landing. Phiên đang tồn tại nhưng đã timeout
        // (ghost) thì không tính là conflict sống (chỉ SOL track keep-alive; network khác isHaveOldSession=false).
        if (_usersIds[userId]?.containsKey(dataType) != true) return false
        return !_checkAlive.isHaveOldSession(userId, dataType, landing)
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

    override fun getUserController(userId: Int, dataType: EnumConstants.DataType, landing: EnumConstants.Landing): IUserController? {
        // Single-mode network: bỏ qua landing.
        return _usersIds[userId]?.get(dataType)
    }

    override fun checkExistence(userController: IUserController): Boolean {
        return _usersNames.containsKey(userController.userName)
    }

    override fun updateKeepAliveTime(userId: Int, dataType: EnumConstants.DataType, landing: EnumConstants.Landing) {
        _checkAlive.updateKeepAliveTime(userId, dataType, landing)
    }

    private fun doJob() {
        clearLoggedOutUsers()
        initUserControllers()
        _checkAlive.checkKeepAlive()

    }

    // MAX_QUEUE inits per one-second tick on a single thread — see the note in LegacyUsersManager.
    private fun initUserControllers() {
        val tickStartMs = System.currentTimeMillis()
        var drained = 0
        while (drained < MAX_QUEUE) {
            val controller = _initQueue.poll() ?: break
            drained++
            val startMs = System.currentTimeMillis()
            val success = controller.initDependencies()
            val tookMs = System.currentTimeMillis() - startMs
            if (!success) {
                _logger.log("[InitFail] disconnect uid=${controller.userId} landing=${controller.landing} took=${tookMs}ms (initDependencies failed)")
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
                    _checkAlive.removeKeepAlive(userId, dataType, controller.landing)
                } else {
                    // Fallback: remove all entries for this user
                    _usersIds.remove(userId)
                }
                controller.disconnect(KickReason.NEED_LOGIN_AGAIN)
            } else if (tookMs >= SLOW_INIT_MS) {
                _logger.warn("[InitQueue] slow init uid=${controller.userId} landing=${controller.landing} took=${tookMs}ms")
            }
        }
        if (drained == 0) {
            return
        }
        val remaining = _initQueue.size
        _logger.log("[InitQueue] drained=$drained remaining=$remaining took=${System.currentTimeMillis() - tickStartMs}ms")
        if (remaining > 0) {
            _logger.warn("[InitQueue] backlog remaining=$remaining after drained=$drained (cap=$MAX_QUEUE/tick)")
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
            val userController = getUserController(userId, dataType, EnumConstants.Landing.WILDCARD)
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
            _checkAlive.removeTimeout(userId, dataType, userController.landing)
        }
        _loggedOutUsers.remove(userId)
        _logger.log("Dispose user ${userController.userName}")
    }
}