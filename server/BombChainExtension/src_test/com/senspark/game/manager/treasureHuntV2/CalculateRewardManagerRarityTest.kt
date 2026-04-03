package com.senspark.game.manager.treasureHuntV2

import com.senspark.game.data.model.config.RewardLevelConfig
import com.senspark.game.data.model.config.TreasureHuntV2Config
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.IHeroDetails
import com.senspark.game.declare.EnumConstants
import com.senspark.game.manager.stake.IHeroStakeManager
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CalculateRewardManagerRarityTest {

    private lateinit var calculateRewardManager: CalculateRewardManager
    private lateinit var heroStakeManagerMock: IHeroStakeManager
    private lateinit var configMock: TreasureHuntV2Config
    private val rewardLevelConfigMock = mapOf<Int, RewardLevelConfig>()

    @BeforeEach
    fun setup() {
        heroStakeManagerMock = mockk(relaxed = true)
        
        // Mock TreasureHuntV2Config with 10 min_stake elements
        // Patterns: Rarities 0-5 = 0.0, 6=1.0, 7=1.3, 8=1.7, 9=2.2
        configMock = mockk(relaxed = true)
        every { configMock.minStake } returns listOf(0, 0, 0, 0, 0, 0, 1, 2, 2, 3) 
        
        calculateRewardManager = CalculateRewardManager(
            heroStakeManagerMock,
            configMock,
            EnumConstants.BLOCK_REWARD_TYPE.BCOIN
        )
        
        calculateRewardManager.setConfig(configMock, rewardLevelConfigMock)
    }

    @Test
    fun testAddHighRarityHeroToPool() {
        val rarity9Hero = mockk<Hero>(relaxed = true)
        val details = mockk<IHeroDetails>(relaxed = true)
        
        every { rarity9Hero.rarity } returns 9 // Super Mystic
        every { rarity9Hero.details } returns details
        every { details.dataType } returns EnumConstants.DataType.BSC
        
        val userId = UserId(1, EnumConstants.DataType.BSC)
        
        // This should NOT throw ArrayIndexOutOfBoundsException now that POOL_SIZE is 10
        calculateRewardManager.addHeroToPool(rarity9Hero, userId, 1)
        
        assertNotNull(calculateRewardManager)
    }

    @Test
    fun testScoreCalculationForRarity9() {
        val rarity9Hero = mockk<Hero>(relaxed = true)
        every { rarity9Hero.rarity } returns 9
        every { rarity9Hero.stakeBcoin } returns 10.0
        
        // minStake offset for rarity 9 is index 9 of our mock list (value 3)
        // Score = (stake - offset) * ticketCount
        // If ticketCount = 1, Score = (10.0 - 3.0) * 1 = 7.0
        
        val userId = UserId(1, EnumConstants.DataType.BSC)
        calculateRewardManager.addHeroToPool(rarity9Hero, userId, 1)
        
        val score = calculateRewardManager.getScore(rarity9Hero)
        assertEquals(7.0, score, "Score calculation for rarity 9 should use the 10th element of minStake array")
    }
}
