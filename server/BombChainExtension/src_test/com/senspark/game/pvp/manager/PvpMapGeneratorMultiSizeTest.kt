package com.senspark.game.pvp.manager

import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.config.IMapConfig
import com.senspark.game.pvp.entity.BlockType
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PvpMapGeneratorMultiSizeTest {

    @Test
    fun `test generate map for 1v1 small`() {
        val config = mockk<IMapConfig>(relaxed = true)
        every { config.mapPatternId } returns "SMALL_1V1"
        every { config.maxPlayers } returns 2
        every { config.tilesetList } returns listOf(1)
        every { config.fallingBlockPatternList } returns listOf(mockk(relaxed = true))
        
        val blockGen = mockk<IBlockGenerator>(relaxed = true)
        val logger = mockk<ILogger>(relaxed = true)
        
        val generator = PvpMapGenerator(
            config, 1 to 1, 0.5f, listOf(BlockType.Chest), listOf(1.0f),
            0.5f, listOf(BlockType.Item), listOf(1.0f), blockGen, logger
        )
        
        val map = generator.generate()
        
        // Assertions based on the standard 15x13 small pattern
        assertEquals(15, map.width)
        assertEquals(13, map.height)
        assertEquals(2, map.startingPositions.size)
        assertNotNull(map.chestBlockArea)
        verify { logger.log(match { it.contains("SMALL_1V1") }) }
    }

    @Test
    fun `test generate map for BR large`() {
        val config = mockk<IMapConfig>(relaxed = true)
        every { config.mapPatternId } returns "LARGE_BR"
        every { config.maxPlayers } returns 6
        every { config.tilesetList } returns listOf(2)
        every { config.fallingBlockPatternList } returns listOf(mockk(relaxed = true))
        
        val blockGen = mockk<IBlockGenerator>(relaxed = true)
        val logger = mockk<ILogger>(relaxed = true)
        
        val generator = PvpMapGenerator(
            config, 3 to 3, 0.5f, listOf(BlockType.Chest), listOf(1.0f),
            0.5f, listOf(BlockType.Item), listOf(1.0f), blockGen, logger
        )
        
        val map = generator.generate()
        
        // Assertions based on the standard 21x21 large pattern
        assertEquals(21, map.width)
        assertEquals(21, map.height)
        assertEquals(6, map.startingPositions.size)
        verify { logger.log(match { it.contains("LARGE_BR") }) }
    }

    @Test
    fun `test tileset randomization`() {
        val config = mockk<IMapConfig>(relaxed = true)
        val tilesets = listOf(10, 20, 30)
        every { config.tilesetList } returns tilesets
        every { config.mapPatternId } returns "SMALL_1V1"
        every { config.fallingBlockPatternList } returns listOf(mockk(relaxed = true))

        val generator = PvpMapGenerator(
            config, 1 to 1, 0.5f, emptyList(), emptyList(),
            0.5f, emptyList(), emptyList(), mockk(relaxed = true), mockk(relaxed = true)
        )

        val map = generator.generate()
        assertTrue(tilesets.contains(map.tileset))
    }
}
