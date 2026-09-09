package com.senspark.game.pvp

import com.senspark.game.pvp.config.ConstantMapConfig
import com.senspark.game.pvp.config.PvpGameMode
import com.senspark.game.pvp.manager.MapPatternRegistry
import com.senspark.game.pvp.manager.PvpMapGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.entity.BlockType
import com.senspark.game.pvp.manager.IBlockGenerator
import io.mockk.mockk

class PvpPhase0Test {

    @Test
    fun testMapRegistry() {
        val smallPattern = MapPatternRegistry.getPattern("SMALL_1V1")
        assertTrue(smallPattern.contains("0"))
        assertTrue(smallPattern.contains("1"))
        
        val largePattern = MapPatternRegistry.getPattern("LARGE_BR")
        assertTrue(largePattern.contains("0"))
        assertTrue(largePattern.contains("5"))
    }

    @Test
    fun testPvpGameMode() {
        val mode = PvpGameMode.BATTLE_ROYALE_6P
        assertEquals(6, mode.maxPlayers)
        assertEquals(1, mode.teamSize)
        assertEquals(4, mode.value)
    }
}
