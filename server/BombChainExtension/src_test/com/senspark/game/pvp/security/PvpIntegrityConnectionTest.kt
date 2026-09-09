package com.senspark.game.pvp.security

import com.senspark.game.api.PvpResultInfo
import com.senspark.common.pvp.PvpMode
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PvpIntegrityConnectionTest {

    @Test
    fun `test integrity signature generation and serialization`() {
        val integrityService = PvpMatchIntegrityService()
        
        // Mocking dependencies for PvpResultInfo serialization
        val result = PvpResultInfo(
            id = "match_secure_test",
            serverId = "server_node_1",
            timestamp = System.currentTimeMillis(),
            mode = PvpMode.NORMAL_1V1,
            isDraw = false,
            winningTeam = 1,
            scores = listOf(1, 0),
            duration = 45,
            rule = mockk(relaxed = true),
            team = emptyList(),
            info = emptyList(),
            wagerMode = 1,
            wagerTier = 1,
            wagerToken = 1
        )
        
        // 1. Generate signature
        integrityService.signResult(result)
        
        assertNotNull(result.signature, "Signature should not be null after signing")
        _root_ide_package_.com.senspark.game.pvp.security.PvpMatchIntegrityService().let {
            // Verify HMAC consistency
            val originalSig = result.signature
            result.signature = null // Clear to re-sign
            integrityService.signResult(result)
            assertEquals(originalSig, result.signature, "Signature should be deterministic for same data")
        }
        
        // 2. Verify Serialization (Connection Test)
        val json = PvpResultInfo.parse(result)
        assertTrue(json.contains("\"signature\":\""), "Serialized JSON must contain signature field")
        assertTrue(json.contains("\"id\":\"match_secure_test\""), "Serialized JSON must contain match ID")
        assertTrue(json.contains("\"wagerMode\":1"), "Serialized JSON must contain wager info")
        
        // 3. Test integrity log inclusion
        result.integrityLogs = "ACTION:MOVE;ACTION:PLANT"
        val jsonWithLogs = PvpResultInfo.parse(result)
        assertTrue(jsonWithLogs.contains("ACTION:MOVE"), "Serialized JSON must contain integrity logs")
    }

    @Test
    fun `test signature detects tampering`() {
        val integrityService = PvpMatchIntegrityService()
        val result = PvpResultInfo(
            id = "tamper_test",
            serverId = "S1",
            timestamp = 1000L,
            mode = PvpMode.NORMAL_1V1,
            isDraw = false,
            winningTeam = 1,
            scores = listOf(1, 0),
            duration = 10,
            rule = mockk(relaxed = true),
            team = emptyList(),
            info = emptyList(),
            wagerMode = 0,
            wagerTier = 0,
            wagerToken = 0
        )

        integrityService.signResult(result)
        val originalSig = result.signature

        // Tamper with data
        result.winningTeam = 2
        result.signature = null
        integrityService.signResult(result)
        
        assertNotEquals(originalSig, result.signature, "Signature must change if data is tampered with")
    }
}
