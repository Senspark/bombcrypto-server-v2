package com.senspark.game.service

import com.senspark.common.IDatabase
import com.senspark.common.IQueryBuilder
import com.senspark.common.utils.ILogger
import com.senspark.game.api.IPvpResultInfo
import com.senspark.game.api.IPvpResultUserInfo
import com.senspark.common.pvp.PvpMode
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.temporal.IsoFields

class PvpRankingWeeklyMonthlyTest {
    private lateinit var db: IDatabase
    private lateinit var logger: ILogger
    private lateinit var service: PvpRankingService
    private lateinit var queryBuilder: IQueryBuilder

    @BeforeEach
    fun setup() {
        db = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        queryBuilder = mockk(relaxed = true)
        
        every { db.createQueryBuilder() } returns queryBuilder
        every { queryBuilder.addStatement(any(), any()) } returns queryBuilder
        
        service = PvpRankingService(db, logger)
    }

    @Test
    fun `test updateRankings for winner increment points by 25`() {
        val resultInfo = mockk<IPvpResultInfo>(relaxed = true)
        val winner = mockk<IPvpResultUserInfo>(relaxed = true)
        
        every { resultInfo.mode } returns PvpMode.NORMAL_1V1
        every { resultInfo.winningTeam } returns 1
        every { resultInfo.isDraw } returns false
        every { resultInfo.info } returns listOf(winner)
        
        every { winner.userId } returns 100
        every { winner.teamId } returns 1
        every { winner.isBot } returns false
        
        service.updateRankings(resultInfo)
        
        // Points should be 25 for winner
        verify { queryBuilder.addStatement(match { it.contains("INSERT INTO pvp_weekly_ranking") }, match { it[4] == 25 }) }
        verify { queryBuilder.addStatement(match { it.contains("INSERT INTO pvp_monthly_ranking") }, match { it[4] == 25 }) }
    }

    @Test
    fun `test updateRankings for loser decrement points by 20`() {
        val resultInfo = mockk<IPvpResultInfo>(relaxed = true)
        val loser = mockk<IPvpResultUserInfo>(relaxed = true)
        
        every { resultInfo.mode } returns PvpMode.NORMAL_1V1
        every { resultInfo.winningTeam } returns 2
        every { resultInfo.isDraw } returns false
        every { resultInfo.info } returns listOf(loser)
        
        every { loser.userId } returns 101
        every { loser.teamId } returns 1
        every { loser.isBot } returns false
        
        service.updateRankings(resultInfo)
        
        // Points should be -20 for loser
        verify { queryBuilder.addStatement(match { it.contains("INSERT INTO pvp_weekly_ranking") }, match { it[4] == -20 }) }
    }

    @Test
    fun `test updateRankings for draw increment points by 5`() {
        val resultInfo = mockk<IPvpResultInfo>(relaxed = true)
        val player = mockk<IPvpResultUserInfo>(relaxed = true)
        
        every { resultInfo.mode } returns PvpMode.NORMAL_1V1
        every { resultInfo.isDraw } returns true
        every { resultInfo.info } returns listOf(player)
        
        every { player.userId } returns 102
        every { player.isBot } returns false
        
        service.updateRankings(resultInfo)
        
        // Points should be 5 for draw
        verify { queryBuilder.addStatement(match { it.contains("INSERT INTO pvp_weekly_ranking") }, match { it[4] == 5 }) }
    }

    @Test
    fun `test bot players are ignored`() {
        val resultInfo = mockk<IPvpResultInfo>(relaxed = true)
        val bot = mockk<IPvpResultUserInfo>(relaxed = true)
        
        every { resultInfo.info } returns listOf(bot)
        every { bot.isBot } returns true
        
        service.updateRankings(resultInfo)
        
        verify(exactly = 0) { queryBuilder.executeUpdate() }
    }
}
