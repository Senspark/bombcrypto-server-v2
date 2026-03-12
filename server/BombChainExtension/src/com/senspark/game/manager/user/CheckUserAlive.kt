package com.senspark.game.manager.user

import com.senspark.common.utils.ILogger
import com.senspark.game.declare.EnumConstants
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class CheckUserAlive(logger: ILogger, timeOut: Long) {
    
    private val _logger = logger
    private val _timeOut = timeOut
    private val _lastKeepAliveTime: ConcurrentHashMap<Int, ConcurrentHashMap<EnumConstants.DataType, Instant>> = ConcurrentHashMap()
    private val _timeOutUserIds: MutableList<Pair<Int, EnumConstants.DataType>> = mutableListOf()
    
    fun addUserToCheck(userId: Int, dataType: EnumConstants.DataType) {
        if (!_lastKeepAliveTime.containsKey(userId)) {
            _lastKeepAliveTime[userId] = ConcurrentHashMap()
        }
        _lastKeepAliveTime[userId]!![dataType] = Instant.now()
    }
    
    fun checkKeepAlive() {
        val now = Instant.now()
        val tempTimeOut = mutableListOf<Pair<Int, EnumConstants.DataType>>()
        // Ko kick nữa chỉ đánh dấu là disconnect thôi, lần login sau sẽ check cái này để xử lý
        _lastKeepAliveTime.forEach { (userId, dataTypeMap) ->
            dataTypeMap.forEach { (dataType, lastKeepAlive) ->
                if (now.minusSeconds(_timeOut) > lastKeepAlive) {
                    tempTimeOut.add(Pair(userId, dataType))
                }
            }
        }

        tempTimeOut.forEach { (userId, dataType) ->
            _logger.log("User with ID $userId (DataType: $dataType) timed out (no keep-alive received within $_timeOut seconds)")
            _timeOutUserIds.add(Pair(userId, dataType))
            _lastKeepAliveTime[userId]?.remove(dataType)
            // If no more data types for this user, remove the entire entry
            if (_lastKeepAliveTime[userId]?.isEmpty() == true) {
                _lastKeepAliveTime.remove(userId)
            }
        }
    }
    
    fun updateKeepAliveTime(userId: Int, dataType: EnumConstants.DataType) {
        if (!_lastKeepAliveTime.containsKey(userId)) {
            _lastKeepAliveTime[userId] = ConcurrentHashMap()
        }
        _lastKeepAliveTime[userId]!![dataType] = Instant.now()
        
        // Remove from timeout list if present for this specific dataType
        _timeOutUserIds.removeIf { it.first == userId && it.second == dataType }
    }
    
    fun isHaveOldSession(userId: Int, dataType: EnumConstants.DataType): Boolean {
        return _timeOutUserIds.contains(Pair(userId, dataType))
    }
    
    fun removeKeepAlive(userId: Int, dataType: EnumConstants.DataType) {
        _lastKeepAliveTime[userId]?.remove(dataType)
        // If no more data types for this user, remove the entire entry
        if (_lastKeepAliveTime[userId]?.isEmpty() == true) {
            _lastKeepAliveTime.remove(userId)
        }
    }
    
    fun removeTimeout(userId: Int, dataType: EnumConstants.DataType) {
        _timeOutUserIds.removeIf { it.first == userId && it.second == dataType }
    }
}