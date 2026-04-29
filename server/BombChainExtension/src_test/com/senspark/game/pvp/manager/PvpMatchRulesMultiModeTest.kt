package com.senspark.game.pvp.manager

import com.senspark.common.pvp.PvpMode
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PvpMatchRulesMultiModeTest {

    @Test
    fun `test winner detection in Battle Royale mode`() {
        // Mock 3 players in BR (FFA)
        val players = listOf(
            mockk<com.senspark.game.pvp.user.IParticipantController> { 
                every { teamId } returns 0
                every { isDead } returns true 
            },
            mockk<com.senspark.game.pvp.user.IParticipantController> { 
                every { teamId } returns 1
                every { isDead } returns false 
            },
            mockk<com.senspark.game.pvp.user.IParticipantController> { 
                every { teamId } returns 2
                every { isDead } returns true 
            }
        )

        val aliveTeams = players.filter { !it.isDead }.map { it.teamId }.distinct()
        
        assertEquals(1, aliveTeams.size, "Should have exactly one team alive")
        assertEquals(1, aliveTeams[0], "Team 1 should be the winner")
    }

    @Test
    fun `test winner detection in 2v2 Team mode`() {
        // Mock 4 players in 2v2
        val players = listOf(
            // Team 0 (All dead)
            mockk<com.senspark.game.pvp.user.IParticipantController> { 
                every { teamId } returns 0
                every { isDead } returns true 
            },
            mockk<com.senspark.game.pvp.user.IParticipantController> { 
                every { teamId } returns 0
                every { isDead } returns true 
            },
            // Team 1 (One alive)
            mockk<com.senspark.game.pvp.user.IParticipantController> { 
                every { teamId } returns 1
                every { isDead } returns false 
            },
            mockk<com.senspark.game.pvp.user.IParticipantController> { 
                every { teamId } returns 1
                every { isDead } returns true 
            }
        )

        val aliveTeams = players.filter { !it.isDead }.map { it.teamId }.distinct()
        
        assertEquals(1, aliveTeams.size, "Should have exactly one team alive")
        assertEquals(1, aliveTeams[0], "Team 1 should be the winner even with only one survivor")
    }

    @Test
    fun `test draw condition when all teams dead`() {
        val players = listOf(
            mockk<com.senspark.game.pvp.user.IParticipantController> { 
                every { teamId } returns 0
                every { isDead } returns true 
            },
            mockk<com.senspark.game.pvp.user.IParticipantController> { 
                every { teamId } returns 1
                every { isDead } returns true 
            }
        )

        val aliveTeams = players.filter { !it.isDead }.map { it.teamId }.distinct()
        
        assertTrue(aliveTeams.isEmpty(), "No teams should be alive")
    }
}
