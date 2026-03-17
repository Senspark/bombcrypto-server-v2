package com.senspark.game.manager.hero

import com.senspark.game.api.IBlockchainDatabaseManager
import com.senspark.game.controller.IUserHouseManager
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.manager.hero.IHeroAbilityConfigManager
import com.senspark.game.data.manager.hero.IHeroBuilder
import com.senspark.game.data.manager.hero.IHeroRepairShieldDataManager
import com.senspark.game.data.manager.treassureHunt.ITreasureHuntConfigManager
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.db.ITHModeDataAccess
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.EnumConstants
import com.senspark.game.manager.blockReward.IUserBlockRewardManager
import com.senspark.game.manager.resourceSync.ISyncResourceManager
import com.senspark.game.manager.stake.IHeroStakeManager
import com.senspark.game.extension.ServerServices
import com.senspark.game.extension.GlobalServices
import com.senspark.lib.data.manager.IGameConfigManager
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class UserHeroFiManagerPaginationTest {

    private lateinit var userHeroFiManager: UserHeroFiManager

    private lateinit var mediatorMock: UserControllerMediator
    private lateinit var houseManagerMock: IUserHouseManager
    private lateinit var userBlockRewardManagerMock: IUserBlockRewardManager
    private lateinit var userDataAccessMock: IUserDataAccess
    private lateinit var gameDataAccessMock: IGameDataAccess
    private lateinit var thModeDataAccessMock: ITHModeDataAccess
    private lateinit var heroAbilityConfigManagerMock: IHeroAbilityConfigManager
    private lateinit var treasureHuntDataManagerMock: ITreasureHuntConfigManager
    private lateinit var gameConfigManagerMock: IGameConfigManager
    private lateinit var heroStakeManagerMock: IHeroStakeManager
    private lateinit var syncResourceManagerMock: ISyncResourceManager
    private lateinit var databaseManagerMock: IBlockchainDatabaseManager
    private lateinit var heroRepairShieldDataManagerMock: IHeroRepairShieldDataManager
    private lateinit var heroBuilderMock: IHeroBuilder
    private lateinit var svServicesMock: ServerServices
    private lateinit var servicesMock: GlobalServices

    @BeforeEach
    fun setup() {
        mediatorMock = mockk(relaxed = true)
        houseManagerMock = mockk(relaxed = true)
        userBlockRewardManagerMock = mockk(relaxed = true)
        userDataAccessMock = mockk(relaxed = true)
        gameDataAccessMock = mockk(relaxed = true)
        thModeDataAccessMock = mockk(relaxed = true)
        heroAbilityConfigManagerMock = mockk(relaxed = true)
        treasureHuntDataManagerMock = mockk(relaxed = true)
        gameConfigManagerMock = mockk(relaxed = true)
        heroStakeManagerMock = mockk(relaxed = true)
        syncResourceManagerMock = mockk(relaxed = true)
        databaseManagerMock = mockk(relaxed = true)
        heroRepairShieldDataManagerMock = mockk(relaxed = true)
        heroBuilderMock = mockk(relaxed = true)
        svServicesMock = mockk(relaxed = true)
        servicesMock = mockk(relaxed = true)

        every { mediatorMock.svServices } returns svServicesMock
        every { mediatorMock.services } returns servicesMock

        // Mocking the behavior of ServiceContainer with get reified function logic
        every { servicesMock.get<IUserDataAccess>() } returns userDataAccessMock
        every { servicesMock.get<IGameDataAccess>() } returns gameDataAccessMock
        every { servicesMock.get<ITHModeDataAccess>() } returns thModeDataAccessMock
        every { servicesMock.get<IHeroAbilityConfigManager>() } returns heroAbilityConfigManagerMock
        every { servicesMock.get<ITreasureHuntConfigManager>() } returns treasureHuntDataManagerMock
        every { servicesMock.get<IGameConfigManager>() } returns gameConfigManagerMock

        every { svServicesMock.get<IHeroStakeManager>() } returns heroStakeManagerMock
        every { svServicesMock.get<ISyncResourceManager>() } returns syncResourceManagerMock
        every { svServicesMock.get<IBlockchainDatabaseManager>() } returns databaseManagerMock
        every { svServicesMock.get<IHeroRepairShieldDataManager>() } returns heroRepairShieldDataManagerMock
        every { svServicesMock.get<IHeroBuilder>() } returns heroBuilderMock

        every { mediatorMock.dataType } returns EnumConstants.DataType.BSC
        every { mediatorMock.userId } returns 1
        every { gameConfigManagerMock.maxBomberActive } returns 15

        userHeroFiManager = UserHeroFiManager(
            mediatorMock,
            houseManagerMock,
            userBlockRewardManagerMock
        )
    }

    @Test
    fun testInitialization() {
        assertNotNull(userHeroFiManager)
    }

    @Test
    fun testStressLoadPagination() {
        val totalHeroes = 10000
        val pageLimit = 100

        mockGetFiHeroesWithPagination(totalHeroes)

        // Measure First Page / Cold Start
        val coldStartStartTime = System.currentTimeMillis()

        // _items must be initialized which normally happens inside getItems() which then calls loadBomberMan()
        // we can simply call getBombermans() to trigger the cold start because it checks if initialized
        // and if not it initializes and calls loadBomberMan internally
        val bombermansColdStart = userHeroFiManager.getBombermans() // Should load 100 initially because of PAGE_LIMIT

        val coldStartEndTime = System.currentTimeMillis()
        val coldStartDuration = coldStartEndTime - coldStartStartTime

        kotlin.test.assertEquals(pageLimit, bombermansColdStart.size)

        var totalLoaded = bombermansColdStart.size

        // Get initial memory usage
        val runtime = Runtime.getRuntime()
        runtime.gc()
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()

        // Measure the remaining pagination loops
        val loopStartTime = System.currentTimeMillis()
        var currentOffset = pageLimit

        while (totalLoaded < totalHeroes) {
            userHeroFiManager.loadMoreHeroes(currentOffset, pageLimit)
            currentOffset += pageLimit
            totalLoaded += pageLimit
        }
        val loopEndTime = System.currentTimeMillis()
        val loopDuration = loopEndTime - loopStartTime

        // Calculate memory usage after loading 10,000 heroes
        runtime.gc()
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsedMb = (memoryAfter - memoryBefore) / (1024 * 1024)

        // Ensure all 10,000 heroes are loaded
        val finalBombermans = userHeroFiManager.getBombermans()
        kotlin.test.assertEquals(totalHeroes, finalBombermans.size)

        // Generate report output
        val report = """
            # 🚀 Stress Test Report: Server Inventory Pagination

            ## Setup
            - **Total Heroes**: $totalHeroes
            - **Page Limit**: $pageLimit

            ## Results
            - **Cold Start Time (First Page - 100 Heroes)**: ${coldStartDuration}ms
            - **Remaining Pagination Time (9,900 Heroes)**: ${loopDuration}ms
            - **Total Execution Time**: ${coldStartDuration + loopDuration}ms
            - **Estimated Memory Used**: ${memoryUsedMb}MB
            - **OOM Status**: OK (No Out-of-Memory exception)
            - **Status**: SUCCESS

            ## Verification
            - Initial page loaded exact $pageLimit heroes.
            - Final heroes count matched exactly $totalHeroes heroes in `UserHeroFiManager`.
        """.trimIndent()

        // Write to root
        java.io.File("../../stress-test-report.md").writeText(report)
        println(report)
    }

    private fun mockGetFiHeroesWithPagination(totalHeroes: Int) {
        every {
            heroBuilderMock.getFiHeroes(any(), any(), any(), any())
        } answers {
            val limit = arg<Int>(2)
            val offset = arg<Int>(3)

            val result = mutableMapOf<Int, com.senspark.game.data.model.nft.Hero>()

            if (offset >= totalHeroes) {
                return@answers result
            }

            val end = Math.min(offset + limit, totalHeroes)

            val shieldMock = mockk<com.senspark.game.data.model.nft.HeroShield>(relaxed = true)
            val helperMock = mockk<com.senspark.game.data.manager.hero.IHeroHelper>(relaxed = true)
            val now = java.time.Instant.now()

            // Instantiate completely standard POJOs without MockK per-item
            for (i in offset until end) {
                val localDetailsMock = mockk<com.senspark.game.data.model.nft.IHeroDetails>(relaxed = true)
                every { localDetailsMock.heroId } returns i
                every { localDetailsMock.type } returns EnumConstants.HeroType.FI

                val heroMock = com.senspark.game.data.model.nft.Hero(
                    userId = 1,
                    _details = localDetailsMock,
                    _active = false,
                    _stage = com.senspark.game.declare.GameConstants.BOMBER_STAGE.SLEEP,
                    _energy = 100,
                    _timeRest = 0L,
                    _shield = shieldMock,
                    _stakeBcoin = 0.0,
                    _stakeSen = 0.0,
                    _lockSince = now,
                    _lockSeconds = 0,
                    _helper = helperMock
                )

                result[i] = heroMock
            }

            result
        }
    }
}
