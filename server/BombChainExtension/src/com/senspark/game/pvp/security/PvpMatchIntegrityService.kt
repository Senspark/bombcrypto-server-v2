package com.senspark.game.pvp.security

import com.senspark.common.pvp.IPvpResultInfo
import com.senspark.common.utils.ILogger
import java.security.MessageDigest
import java.util.*

/**
 * Service responsible for ensuring the integrity of a match by logging all actions
 * and signing the final result to prevent tampering.
 */
class PvpMatchIntegrityService(
    private val _matchId: String,
    private val _logger: ILogger
) {
    private val _actionLogs = Collections.synchronizedList(mutableListOf<String>())
    
    /**
     * Logs an action taken by a participant for future replay or audit.
     */
    fun logAction(slot: Int, action: String, details: String) {
        val timestamp = System.currentTimeMillis()
        val entry = "$timestamp|$slot|$action|$details"
        _actionLogs.add(entry)
    }
    
    /**
     * Generates a digital signature for the match result.
     * In a production environment, this would use an RSA/ECDSA private key.
     * For now, we use a SHA-256 hash with a secret salt.
     */
    fun signResult(result: IPvpResultInfo): String {
        val secretSalt = "BOMB_PVP_SECURE_2025"
        val dataToSign = StringBuilder()
            .append(result.id).append("|")
            .append(result.winningTeam).append("|")
            .append(result.isDraw).append("|")
            .append(result.duration).append("|")
            .append(secretSalt)
            .toString()
        
        return hashString(dataToSign)
    }
    
    private fun hashString(input: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Exports the full log of actions as a single string (CSV-like format).
     */
    fun exportLogs(): String {
        return _actionLogs.joinToString("\n")
    }

    fun clear() {
        _actionLogs.clear()
    }
}
