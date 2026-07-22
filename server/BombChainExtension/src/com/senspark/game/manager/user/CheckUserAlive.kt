package com.senspark.game.manager.user

import com.senspark.common.utils.ILogger
import com.senspark.game.declare.EnumConstants
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class CheckUserAlive(logger: ILogger, timeOut: Long) {

    // Keep-alive được theo dõi theo từng phiên = (uid, dataType, landing) để 1 account FI mở
    // đồng thời Treasure + Adventure: tab này timeout không làm dispose nhầm tab kia.
    private data class SessionKey(val dataType: EnumConstants.DataType, val landing: EnumConstants.Landing)

    private val _logger = logger
    private val _timeOut = timeOut
    private val _lastKeepAliveTime: ConcurrentHashMap<Int, ConcurrentHashMap<SessionKey, Instant>> = ConcurrentHashMap()
    private val _timeOutUserIds: MutableSet<Pair<Int, SessionKey>> = ConcurrentHashMap.newKeySet()

    fun addUserToCheck(userId: Int, dataType: EnumConstants.DataType, landing: EnumConstants.Landing) {
        if (!_lastKeepAliveTime.containsKey(userId)) {
            _lastKeepAliveTime[userId] = ConcurrentHashMap()
        }
        _lastKeepAliveTime[userId]!![SessionKey(dataType, landing)] = Instant.now()
    }

    fun checkKeepAlive() {
        val now = Instant.now()
        val tempTimeOut = mutableListOf<Pair<Int, SessionKey>>()
        // Ko kick nữa chỉ đánh dấu là disconnect thôi, lần login sau sẽ check cái này để xử lý
        _lastKeepAliveTime.forEach { (userId, sessionMap) ->
            sessionMap.forEach { (sessionKey, lastKeepAlive) ->
                if (now.minusSeconds(_timeOut) > lastKeepAlive) {
                    tempTimeOut.add(Pair(userId, sessionKey))
                }
            }
        }

        tempTimeOut.forEach { (userId, sessionKey) ->
            _logger.log("User with ID $userId (DataType: ${sessionKey.dataType}, Landing: ${sessionKey.landing}) timed out (no keep-alive received within $_timeOut seconds)")
            _timeOutUserIds.add(Pair(userId, sessionKey))
            _lastKeepAliveTime[userId]?.remove(sessionKey)
            // If no more sessions for this user, remove the entire entry
            if (_lastKeepAliveTime[userId]?.isEmpty() == true) {
                _lastKeepAliveTime.remove(userId)
            }
        }
    }

    fun updateKeepAliveTime(userId: Int, dataType: EnumConstants.DataType, landing: EnumConstants.Landing) {
        if (!_lastKeepAliveTime.containsKey(userId)) {
            _lastKeepAliveTime[userId] = ConcurrentHashMap()
        }
        val sessionKey = SessionKey(dataType, landing)
        _lastKeepAliveTime[userId]!![sessionKey] = Instant.now()

        // Remove from timeout list if present for this specific session
        _timeOutUserIds.removeIf { it.first == userId && it.second == sessionKey }
    }

    fun isHaveOldSession(userId: Int, dataType: EnumConstants.DataType, landing: EnumConstants.Landing): Boolean {
        return _timeOutUserIds.contains(Pair(userId, SessionKey(dataType, landing)))
    }

    // Snapshot các phiên đã timeout (uid, dataType, landing) để manager evict ghost chủ động khỏi _usersIds.
    fun getTimedOutSessions(): List<Triple<Int, EnumConstants.DataType, EnumConstants.Landing>> {
        return _timeOutUserIds.map { (userId, key) -> Triple(userId, key.dataType, key.landing) }
    }

    fun removeKeepAlive(userId: Int, dataType: EnumConstants.DataType, landing: EnumConstants.Landing) {
        _lastKeepAliveTime[userId]?.remove(SessionKey(dataType, landing))
        // If no more sessions for this user, remove the entire entry
        if (_lastKeepAliveTime[userId]?.isEmpty() == true) {
            _lastKeepAliveTime.remove(userId)
        }
    }

    fun removeTimeout(userId: Int, dataType: EnumConstants.DataType, landing: EnumConstants.Landing) {
        val sessionKey = SessionKey(dataType, landing)
        _timeOutUserIds.removeIf { it.first == userId && it.second == sessionKey }
    }
}
