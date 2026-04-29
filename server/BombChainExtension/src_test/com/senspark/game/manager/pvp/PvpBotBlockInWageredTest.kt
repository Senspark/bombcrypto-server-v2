package com.senspark.game.manager.pvp

import com.senspark.game.api.IPvpJoinQueueInfo
import com.senspark.game.exception.CustomException
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PvpBotBlockInWageredTest {

    @Test
    fun `test anti-smurf protection for wagered matches`() {
        val logger = mockk<com.senspark.common.utils.ILogger>(relaxed = true)
        val env = mockk<com.senspark.game.manager.IEnvManager>(relaxed = true)
        val scheduler = mockk<com.senspark.common.service.IScheduler>(relaxed = true)
        val timeManager = mockk<com.senspark.game.pvp.manager.ITimeManager>(relaxed = true)
        val listener = mockk<IMatchmakerListener>(relaxed = true)
        val messenger = mockk<com.senspark.common.cache.IMessengerService>(relaxed = true)
        val cache = mockk<com.senspark.common.cache.ICacheService>(relaxed = true)
        val usersManager = mockk<com.senspark.game.manager.IUsersManager>(relaxed = true)
        val pvpDataAccess = mockk<com.senspark.game.service.IPvpDataAccess>(relaxed = true)

        val matchmaker = GlobalMatchmaker(
            _logger = logger,
            _envManager = env,
            _scheduler = scheduler,
            _timeManager = timeManager,
            _configs = emptyList(),
            _listener = listener,
            _messengerService = messenger,
            _cache = cache,
            _usersManager = usersManager,
            _pvpDataAccess = pvpDataAccess
        )
        
        val joinInfo = mockk<IPvpJoinQueueInfo>(relaxed = true)
        every { joinInfo.username } returns "new_player"
        every { joinInfo.wagerMode } returns 1 // Wagered Mode
        
        // Mock user has only 2 free matches (threshold is 10)
        every { usersManager.getUserId("new_player") } returns 999
        every { pvpDataAccess.getTotalPvpMatches(999) } returns 2
        
        val exception = assertThrows(CustomException::class.java) {
            matchmaker.join(joinInfo)
        }
        
        assertTrue(exception.message!!.contains("needs at least 10 free matches"), "Should block new accounts from wagering")
    }

    @Test
    fun `test allowed wagering for experienced players`() {
        val logger = mockk<com.senspark.common.utils.ILogger>(relaxed = true)
        val env = mockk<com.senspark.game.manager.IEnvManager>(relaxed = true)
        val scheduler = mockk<com.senspark.common.service.IScheduler>(relaxed = true)
        val timeManager = mockk<com.senspark.game.pvp.manager.ITimeManager>(relaxed = true)
        val listener = mockk<IMatchmakerListener>(relaxed = true)
        val messenger = mockk<com.senspark.common.cache.IMessengerService>(relaxed = true)
        val cache = mockk<com.senspark.common.cache.ICacheService>(relaxed = true)
        val usersManager = mockk<com.senspark.game.manager.IUsersManager>(relaxed = true)
        val pvpDataAccess = mockk<com.senspark.game.service.IPvpDataAccess>(relaxed = true)

        val matchmaker = GlobalMatchmaker(logger, env, scheduler, timeManager, emptyList(), listener, messenger, cache, usersManager, pvpDataAccess)
        
        val joinInfo = mockk<IPvpJoinQueueInfo>(relaxed = true)
        every { joinInfo.username } returns "pro_player"
        every { joinInfo.wagerMode } returns 1
        
        // Mock user has 50 matches
        every { usersManager.getUserId("pro_player") } returns 100
        every { pvpDataAccess.getTotalPvpMatches(100) } returns 50
        
        // Should not throw exception
        assertDoesNotThrow {
            matchmaker.join(joinInfo)
        }
    }
}
