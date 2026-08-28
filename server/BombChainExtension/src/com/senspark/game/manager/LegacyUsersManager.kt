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
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration.Companion.seconds

private const val K_SESSION_KEY = "name"
private const val MAX_QUEUE = 5
// One init above this eats a fifth of the tick's budget on its own — worth naming the user.
private const val SLOW_INIT_MS = 200L
// 90s thay vì 15s: trình duyệt throttle timer JS của tab nền (ping giãn tới ~15s, intensive throttling tới ~1/phút)
// nên 15s evict nhầm tab nền còn sống (socket SFS vẫn mở). Tab đóng thật vẫn được SFS USER_DISCONNECT dọn ngay.
private val KEEP_ALIVE_TIMEOUT = 90.seconds.inWholeSeconds

class LegacyUsersManager(logger: ILogger) : IUsersManager {
    // key = sessionKey(userName, landing): 1 account FI có thể mở Treasure + Adventure cùng lúc nên
    // username (đã suffix theo network) là như nhau giữa 2 tab; phải thêm landing mới phân biệt được phiên.
    private val _usersNames: ConcurrentHashMap<String, IUserController> = ConcurrentHashMap()
    // uid -> dataType -> landing -> controller
    private val _usersIds: ConcurrentHashMap<Int, ConcurrentHashMap<EnumConstants.DataType, ConcurrentHashMap<EnumConstants.Landing, IUserController>>> = ConcurrentHashMap()
    // Concurrent, not LinkedList: added on the join-zone thread, polled on this manager's scheduler
    // thread — an unsynchronized LinkedList across threads can drop the node, stranding that user uninitialized.
    private val _initQueue: Queue<IUserController> = ConcurrentLinkedQueue()
    private val _scheduler: IScheduler = SmartFoxScheduler(1, logger)
    private val _logger = logger

    // Thread-safe set of UIDs that have client logging enabled (written by admin-command thread, read by request threads)
    private val _clientLoggingEnabledUids: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    private val _checkAlive = CheckUserAlive(logger, KEEP_ALIVE_TIMEOUT)

    // Striped lock theo (uid, dataType): admission (check+resolve+insert) và removeFromMaps chạy atomic
    // trong cùng stripe để diệt race CHECK@login vs REGISTER@join và insert/remove. ReentrantLock để
    // disposeUser/removeFromMaps gọi lồng trong admission không deadlock.
    private val _stripeLocks = Array(64) { ReentrantLock() }
    private fun lockFor(userId: Int, dataType: EnumConstants.DataType): ReentrantLock {
        val h = userId * 31 + dataType.ordinal
        return _stripeLocks[(h and 0x7fffffff) % _stripeLocks.size]
    }

    // Va chạm landing theo bảng xô: WILDCARD đụng mọi landing; specific đụng cùng-loại (và WILDCARD).
    private fun collides(a: EnumConstants.Landing, b: EnumConstants.Landing): Boolean {
        return a == EnumConstants.Landing.WILDCARD || b == EnumConstants.Landing.WILDCARD || a == b
    }

    private fun sessionKey(userName: String, landing: EnumConstants.Landing): String = "$userName#${landing.name}"

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
        landing: EnumConstants.Landing,
        forceLogin: Boolean,
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
        userController.landing = landing
        val initSuccess = userController.verifyAndUpdateUserHash()
        if (!initSuccess) {
            extension.api.disconnectUser(user, KickReason.NEED_LOGIN_AGAIN)
            onCompleted(null)
            return
        }

        val userId = userInfo.id
        val dataType = userInfo.dataType
        val key = sessionKey(userName, landing)

        // ATOMIC ADMISSION — nguồn chân lý của collision-enforcement. Dưới stripe-lock (uid, dataType),
        // gói check + (takeover / reclaim ghost / kick force / reject) + insert thành 1 thao tác atomic.
        // 2 pha: quyết định cho TẤT CẢ conflict trước, chỉ mutate khi chắc chắn admit (tránh dispose-rồi-reject).
        val admitted = lockFor(userId, dataType).withLock {
            val bucket = _usersIds[userId]?.get(dataType)
            // (controller cần dọn, có cần disconnect/kick không)
            val toResolve = mutableListOf<Pair<IUserController, Boolean>>()
            if (bucket != null) {
                for ((existingLanding, existingController) in bucket) {
                    if (!collides(landing, existingLanding)) continue
                    when {
                        // Cùng slot (gồm WILDCARD==WILDCARD): refresh/reconnect/đổi-thiết-bị cùng-mode -> TAKEOVER.
                        // SFS-core đã đá phiên cũ trùng tên nên chỉ cần dispose khỏi map (không disconnect lại).
                        existingLanding == landing -> {
                            _logger.log("[Admission] TAKEOVER same-slot existing=$existingLanding (uid=$userId dt=$dataType)")
                            toResolve.add(existingController to false)
                        }
                        // Khác slot va chạm (dính WILDCARD): ghost -> reclaim (dispose, không kick).
                        _checkAlive.isHaveOldSession(userId, dataType, existingLanding) -> {
                            _logger.log("[Admission] RECLAIM ghost cross-slot existing=$existingLanding new=$landing (uid=$userId dt=$dataType)")
                            toResolve.add(existingController to false)
                        }
                        // Khác slot, phiên cũ còn sống: force_login -> kick; else -> reject phiên mới.
                        forceLogin -> {
                            _logger.log("[Admission] KICK force cross-slot existing=$existingLanding new=$landing (uid=$userId dt=$dataType)")
                            toResolve.add(existingController to true)
                        }
                        else -> {
                            _logger.log("[Admission] REJECT new=$landing vì existing=$existingLanding còn sống (uid=$userId dt=$dataType)")
                            return@withLock false
                        }
                    }
                }
            }
            // Hết conflict cần reject -> dọn phiên cũ rồi đăng ký phiên mới (re-get bucket vì dispose có thể đã xoá).
            toResolve.forEach { (controller, kick) ->
                if (kick) kickAndRemoveUser(controller, "admission-kick-force") else disposeUser(controller, "admission-takeover/reclaim")
            }
            user.setProperty(K_SESSION_KEY, key)
            _usersNames[key] = userController
            _usersIds.getOrPut(userId) { ConcurrentHashMap() }
                .getOrPut(dataType) { ConcurrentHashMap() }[landing] = userController
            _checkAlive.addUserToCheck(userId, dataType, landing)
            true
        }

        if (!admitted) {
            // Account đang có phiên sống va chạm (không ghost, không force_login) -> từ chối phiên mới.
            userController.dispose()
            extension.api.disconnectUser(user, KickReason.USER_LOGGED_IN)
            onCompleted(null)
            return
        }

        userController.setUser(user)
        _initQueue.add(userController)
        _logger.log("[InitQueue] enqueue uid=$userId dt=$dataType landing=$landing queued=${_initQueue.size}")

        onCompleted(userController)
    }

    override fun remove(userName: String) {
        // CẢNH BÁO: getUserController(userName) trả first-match theo userName; 2 tab cùng account có userName y hệt
        // -> có thể dispose NHẦM tab. Log để soi nếu đường này được gọi.
        getUserController(userName)?.let {
            _logger.log("[Remove] by userName=$userName -> first-match landing=${it.landing} (CẢNH BÁO: có thể nhầm tab)")
            disposeUser(it, "remove(userName=$userName)")
        }
    }

    override fun remove(userController: IUserController) {
        disposeUser(userController, "remove(controller)")
    }

    override fun kickAndRemoveUser(userId: Int) {
        getAllUserControllersOfUid(userId).forEach { kickAndRemoveUser(it, "kickAndRemoveUser(uid=$userId)") }
    }

    override fun kickAndRemoveUser(userName: String) {
        val controller = getUserController(userName)
        if (controller != null) {
            _logger.log("[Kick] by userName=$userName -> first-match landing=${controller.landing} (CẢNH BÁO: có thể nhầm tab)")
            kickAndRemoveUser(controller, "kickAndRemoveUser(userName=$userName)")
        }
    }

    override fun isUserLoggedOut(userId: Int, dataType: EnumConstants.DataType): Boolean {
        return true;
    }

    override fun hasLiveConflict(userId: Int, dataType: EnumConstants.DataType, landing: EnumConstants.Landing): Boolean {
        val byLanding = _usersIds[userId]?.get(dataType) ?: return false
        // Chỉ tính phiên còn sống: ghost (đã timeout) và mồ côi (mất bản ghi keep-alive) đều để
        // admission reclaim, không reject oan.
        return byLanding.keys.any { existing ->
            collides(landing, existing) &&
                _checkAlive.hasKeepAlive(userId, dataType, existing) &&
                !_checkAlive.isHaveOldSession(userId, dataType, existing)
        }
    }

    private fun kickAndRemoveUser(userController: IUserController, reason: String = "") {
        _logger.log("[Kick] disconnect uid=${userController.userId} landing=${userController.landing} reason=$reason")
        userController.disconnect(KickReason.KICK)
        disposeUser(userController, reason)
    }

    override fun getUserId(userName: String): Int {
        return getUserController(userName)?.userId ?: -1
    }

    override fun getUserController(userName: String): IUserController? {
        // Tra theo username thô (không có landing) -> trả phiên bất kỳ khớp username.
        return _usersNames.values.firstOrNull { it.userName == userName }
    }

    override fun getUserController(user: User): IUserController? {
        val key = user.getProperty(K_SESSION_KEY) as String? ?: return null
        return _usersNames[key]
    }

    override fun getUserController(userId: Int, dataType: EnumConstants.DataType, landing: EnumConstants.Landing): IUserController? {
        val byLanding = _usersIds[userId]?.get(dataType) ?: return null
        // Ưu tiên đúng landing; nếu không có thì rơi về phiên WILDCARD (client cũ chưa migrate);
        // query WILDCARD = "phiên bất kỳ" của (uid, dataType).
        return byLanding[landing]
            ?: byLanding[EnumConstants.Landing.WILDCARD]
            ?: if (landing == EnumConstants.Landing.WILDCARD) byLanding.values.firstOrNull() else null
    }

    override fun checkExistence(userController: IUserController): Boolean {
        return _usersNames.containsKey(sessionKey(userController.userName, userController.landing))
    }

    // Method to update keep-alive time for a user
    override fun updateKeepAliveTime(userId: Int, dataType: EnumConstants.DataType, landing: EnumConstants.Landing) {
        _checkAlive.updateKeepAliveTime(userId, dataType, landing)
    }

    private fun doJob() {
        initUserControllers()
        _checkAlive.checkKeepAlive()
        evictTimedOutSessions()
        evictOrphanSessions()
    }

    // Slot mồ côi không bao giờ bị đánh timeout nên evictTimedOutSessions không với tới.
    // Re-check dưới lock: admission thêm slot và bản ghi keep-alive trong cùng stripe-lock.
    private fun evictOrphanSessions() {
        val snapshot = _usersIds.entries.flatMap { (userId, byDataType) ->
            byDataType.entries.flatMap { (dataType, byLanding) ->
                byLanding.keys.map { Triple(userId, dataType, it) }
            }
        }
        for ((userId, dataType, landing) in snapshot) {
            lockFor(userId, dataType).withLock {
                val controller = _usersIds[userId]?.get(dataType)?.get(landing) ?: return@withLock
                if (_checkAlive.hasKeepAlive(userId, dataType, landing)) return@withLock
                // Đã timeout thì để evictTimedOutSessions lo, tránh log/kick trùng.
                if (_checkAlive.isHaveOldSession(userId, dataType, landing)) return@withLock
                _logger.warn("Evict orphan session uid=$userId dt=$dataType landing=$landing (slot còn nhưng mất bản ghi keep-alive)")
                kickAndRemoveUser(controller, "evict-orphan-no-keepalive")
            }
        }
    }

    // Evict ghost chủ động: phiên timeout bị gỡ thẳng khỏi _usersIds để hasLiveConflict không còn thấy ma.
    // Re-check isHaveOldSession DƯỚI LOCK trước khi kick — admission có thể đã reclaim & thay phiên mới
    // vào đúng slot (cùng landing); khi đó flag đã bị removeTimeout xoá -> bỏ qua, không kick nhầm phiên mới.
    private fun evictTimedOutSessions() {
        for ((userId, dataType, landing) in _checkAlive.getTimedOutSessions()) {
            lockFor(userId, dataType).withLock {
                if (!_checkAlive.isHaveOldSession(userId, dataType, landing)) return@withLock
                val controller = _usersIds[userId]?.get(dataType)?.get(landing) ?: run {
                    _checkAlive.removeTimeout(userId, dataType, landing)
                    return@withLock
                }
                _logger.log("Evict ghost session uid=$userId dt=$dataType landing=$landing")
                kickAndRemoveUser(controller, "evict-ghost-timeout")
            }
        }
    }


    // At most MAX_QUEUE inits per one-second tick, all on this manager's single scheduler thread — the
    // login funnel is capped at 5 users/s and a slow initDependencies delays everyone behind it.
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
                removeFromMaps(controller)
                controller.disconnect(KickReason.NEED_LOGIN_AGAIN)
            } else if (tookMs >= SLOW_INIT_MS) {
                _logger.warn("[InitQueue] slow init uid=${controller.userId} landing=${controller.landing} took=${tookMs}ms")
            }
        }
        if (drained == 0) {
            return
        }
        val remaining = _initQueue.size
        val tickMs = System.currentTimeMillis() - tickStartMs
        _logger.log("[InitQueue] drained=$drained remaining=$remaining took=${tickMs}ms")
        // Persistent warnings here mean MAX_QUEUE is the bottleneck.
        if (remaining > 0) {
            _logger.warn("[InitQueue] backlog remaining=$remaining after drained=$drained (cap=$MAX_QUEUE/tick)")
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
        return _usersIds[uid]?.values?.flatMap { it.values } ?: emptyList()
    }

    // When a user is disposed, we should also remove them from the logging list
    private fun disposeUser(userController: IUserController, reason: String = "") {
        // Log kèm landing+dataType: userName 2 tab giống hệt nên thiếu landing là không biết tab nào bị dispose.
        _logger.log("[Dispose] uid=${userController.userId} dt=${userController.userInfo?.dataType} landing=${userController.landing} reason=$reason")
        userController.dispose()
        removeFromMaps(userController)
    }

    // Gỡ controller khỏi mọi map theo đúng phiên (uid, dataType, landing). Lấy CÙNG stripe-lock với admission
    // để hết đua insert/remove. Chỉ gỡ khi slot/_usersNames vẫn TRỎ ĐÚNG controller này (reference-equality) —
    // tránh gỡ nhầm phiên mới vừa takeover cùng sessionKey (same userName+landing) hoặc cùng slot.
    private fun removeFromMaps(userController: IUserController) {
        val landing = userController.landing
        val userId = userController.userId
        val dataType = userController.userInfo?.dataType
        if (dataType == null) {
            // Fallback hiếm (thiếu dataType): dọn toàn bộ theo uid.
            _usersIds.remove(userId)
            _usersNames.remove(sessionKey(userController.userName, landing), userController)
            return
        }
        lockFor(userId, dataType).withLock {
            val nameKey = sessionKey(userController.userName, landing)
            if (_usersNames[nameKey] === userController) {
                _usersNames.remove(nameKey)
            } else {
                _logger.log("[RemoveMaps] SKIP _usersNames key=$nameKey (đã trỏ controller khác - tránh gỡ nhầm phiên mới)")
            }
            val byLanding = _usersIds[userId]?.get(dataType)
            if (byLanding?.get(landing) === userController) {
                byLanding.remove(landing)
                // Trong guard: ngoài guard thì dispose muộn của phiên cũ xoá mất keep-alive của phiên
                // mới vừa takeover cùng slot.
                _checkAlive.removeKeepAlive(userId, dataType, landing)
                _checkAlive.removeTimeout(userId, dataType, landing)
            } else {
                // Hiếm: guard reference-equality chặn gỡ nhầm phiên mới vừa takeover cùng slot. Giữ log để soi nếu lệch.
                _logger.log("[RemoveMaps] SKIP slot uid=$userId dt=$dataType landing=$landing (slot đã trỏ controller khác)")
            }
            if (byLanding?.isEmpty() == true) {
                _usersIds[userId]?.remove(dataType)
            }
            if (_usersIds[userId]?.isEmpty() == true) {
                _usersIds.remove(userId)
            }
        }
    }
}
