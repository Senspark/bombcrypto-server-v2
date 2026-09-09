package com.senspark.game.service

import com.senspark.common.IDatabase
import com.senspark.common.IQueryBuilder
import com.senspark.common.utils.ILogger
import com.senspark.game.api.IPvpResultInfo
import com.senspark.game.api.IPvpResultUserInfo
import com.senspark.game.pvp.config.PvpWagerTier
import com.senspark.game.pvp.config.PvpWagerToken
import com.senspark.common.pvp.PvpMode
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PvpWagerServiceTest {
    private lateinit var db: IDatabase
    private lateinit var logger: ILogger
    private lateinit var statement: DatabaseStatement
    private lateinit var service: PvpWagerService
    private lateinit var queryBuilder: IQueryBuilder

    @BeforeEach
    fun setup() {
        db = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        statement = DatabaseStatement(
            queryEvent = "", queryLogPlayPvP = "", queryPvPRank = "",
            queryPvPRankingPoint = "", updateData = "", updatePvPRankingPoint = "",
            queryPvpRankingConfig = "", addUserReward = "ADD_REWARD", subUserReward = "SUB_REWARD"
        )
        queryBuilder = mockk(relaxed = true)
        
        every { db.createQueryBuilder(any()) } returns queryBuilder
        every { queryBuilder.addStatement(any(), any()) } returns queryBuilder
        
        service = PvpWagerService(db, logger, statement)
    }

    @Test
    fun `test debitEscrow success`() {
        val result = service.debitEscrow("match_1", 1001, PvpWagerTier.TIER_10, PvpWagerToken.BCOIN_BSC)
        
        assertTrue(result)
        verify { queryBuilder.addStatement("SUB_REWARD", any()) }
        verify { queryBuilder.addStatement(match { it.contains("INSERT INTO pvp_wager_entry") }, any()) }
        verify { queryBuilder.addStatement(match { it.contains("INSERT INTO pvp_wager_pool") }, any()) }
    }

    @Test
    fun `test debitEscrow failure`() {
        every { queryBuilder.executeUpdate() } throws Exception("DB Fail")
        
        val result = service.debitEscrow("match_1", 1001, PvpWagerTier.TIER_10, PvpWagerToken.BCOIN_BSC)
        
        assertFalse(result)
        verify { logger.error(any(), any()) }
    }

    @Test
    fun `test lockPool success`() {
        val result = service.lockPool("match_1")
        
        assertTrue(result)
        verify { queryBuilder.addStatement(match { it.contains("UPDATE pvp_wager_pool") }, any()) }
        verify { queryBuilder.addStatement(match { it.contains("UPDATE pvp_wager_entry") }, any()) }
    }

    @Test
    fun `test distributePrize TeamMode`() {
        val resultInfo = mockk<IPvpResultInfo>(relaxed = true)
        every { resultInfo.id } returns "match_1"
        every { resultInfo.mode } returns PvpMode.NORMAL_1V1
        every { resultInfo.winningTeam } returns 1
        every { resultInfo.isDraw } returns false
        
        val winner = mockk<IPvpResultUserInfo>(relaxed = true)
        every { winner.userId } returns 2001
        every { winner.teamId } returns 1
        every { winner.quit } returns false
        
        every { resultInfo.info } returns listOf(winner)
        
        // Mock DB result for pool
        val resultSet = mockk<java.sql.ResultSet>(relaxed = true)
        every { resultSet.next() } returns true andThen false
        every { resultSet.getString("token_type") } returns "BCOIN"
        every { resultSet.getString("network") } returns "BSC"
        every { resultSet.getDouble("total_pool") } returns 10.0
        every { resultSet.getDouble("fee_amount") } returns 0.5
        
        // Use a slot for the lambda capture
        val lambdaSlot = slot<(java.sql.ResultSet) -> Unit>()
        every { queryBuilder.executeQuery(capture(lambdaSlot)) } answers {
            lambdaSlot.captured.invoke(resultSet)
        }

        val result = service.distributePrize(resultInfo)
        
        assertTrue(result)
        verify { queryBuilder.addStatement("ADD_REWARD", any()) }
        verify { queryBuilder.addStatement(match { it.contains("UPDATE pvp_wager_entry") && it.contains("WON") }, any()) }
        verify { queryBuilder.addStatement(match { it.contains("INSERT INTO pvp_fee_ledger") }, any()) }
    }
}
