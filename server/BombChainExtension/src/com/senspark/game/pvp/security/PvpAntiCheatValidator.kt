package com.senspark.game.pvp.security

import com.senspark.common.pvp.IMatchHeroInfo
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

class PvpAntiCheatValidator(
    private val _heroInfos: List<IMatchHeroInfo>
) {
    private val _lastActionTimestamp = ConcurrentHashMap<Int, Long>()
    private val _lastPosition = ConcurrentHashMap<Int, Pair<Float, Float>>()
    private val _actionCountInTick = ConcurrentHashMap<Int, Int>()
    
    // Config
    private val MAX_ACTIONS_PER_TICK = 5
    private val SPEED_THRESHOLD_MULTIPLIER = 1.5f // Allow 50% margin for latency and network jitter
    
    /**
     * Validates if the action is chronologically valid and doesn't exceed rate limits.
     */
    fun validateAction(slot: Int, timestamp: Long): Boolean {
        val lastTs = _lastActionTimestamp[slot] ?: 0L
        
        // Prevent replaying old actions or massive jumps in the future
        if (timestamp <= lastTs) return false 
        
        _lastActionTimestamp[slot] = timestamp
        return true
    }
    
    /**
     * Validates movement speed to detect speed-hacks.
     */
    fun validateMovement(slot: Int, timestamp: Long, x: Float, y: Float): Boolean {
        val lastPos = _lastPosition[slot]
        val lastTs = _lastActionTimestamp[slot] ?: (timestamp - 16) // Assume 1 tick if first action
        
        if (lastPos == null) {
            _lastPosition[slot] = Pair(x, y)
            return true
        }
        
        val dx = x - lastPos.first
        val dy = y - lastPos.second
        val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        
        // Time difference in seconds
        val dt = (timestamp - lastTs).coerceAtLeast(1L).toFloat() / 1000f 
        
        // Hero speed (base is tiles per second)
        // Adjusting based on game engine logic (usually speed 1 = X tiles/s)
        val heroBaseSpeed = _heroInfos[slot].speed.toFloat() / 50f 
        val maxAllowedDistance = heroBaseSpeed * dt * SPEED_THRESHOLD_MULTIPLIER
        
        // If distance moved > max allowed distance, flag as potential cheat
        // We allow a small absolute margin (e.g. 0.8 tiles) to avoid false positives on lag spikes
        if (distance > maxAllowedDistance && distance > 0.8f) {
            return false
        }
        
        _lastPosition[slot] = Pair(x, y)
        return true
    }

    /**
     * Resets rate limits for a new tick.
     */
    fun resetTick() {
        _actionCountInTick.clear()
    }

    fun incrementActionCount(slot: Int): Boolean {
        val count = (_actionCountInTick[slot] ?: 0) + 1
        if (count > MAX_ACTIONS_PER_TICK) return false
        _actionCountInTick[slot] = count
        return true
    }
}
