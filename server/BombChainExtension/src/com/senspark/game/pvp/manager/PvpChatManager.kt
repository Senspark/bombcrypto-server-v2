package com.senspark.game.pvp.manager

import com.senspark.common.utils.ILogger
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.User
import java.util.concurrent.ConcurrentHashMap

interface IPvpChatManager {
    fun canSendMessage(user: User): Boolean
    fun onMessageSent(user: User)
    fun filterMessage(message: String): String
}

class PvpChatManager(private val _logger: ILogger) : IPvpChatManager {
    private val _lastMessageTimes = ConcurrentHashMap<Int, Long>()
    private val COOLDOWN_MS = 1000L

    override fun canSendMessage(user: User): Boolean {
        val now = System.currentTimeMillis()
        val lastTime = _lastMessageTimes[user.id] ?: 0L
        
        if (now - lastTime < COOLDOWN_MS) {
            return false
        }
        return true
    }

    override fun onMessageSent(user: User) {
        _lastMessageTimes[user.id] = System.currentTimeMillis()
    }

    override fun filterMessage(message: String): String {
        // Simple placeholder for anti-spam/profanity filter
        // In a real scenario, this would use a library or a list of blocked words
        return message.take(200) // Limit length
    }
}
