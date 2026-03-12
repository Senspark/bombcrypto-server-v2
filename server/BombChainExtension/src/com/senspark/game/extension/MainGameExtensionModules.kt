package com.senspark.game.extension

import com.senspark.common.IDatabase
import com.senspark.common.cache.ICacheService
import com.senspark.common.cache.IMessengerService
import com.senspark.common.cache.RedisServices
import com.senspark.common.service.IScheduler
import com.senspark.common.utils.*
import com.senspark.game.api.*
import com.senspark.game.data.manager.autoMine.AutoMineManager
import com.senspark.game.data.manager.block.*
import com.senspark.game.data.manager.hero.*
import com.senspark.game.data.manager.treassureHunt.TreasureHuntConfigManager
import com.senspark.game.db.*
import com.senspark.game.db.cache.CachedUserDataAccess
import com.senspark.game.db.dailyMission.CachedMissionDataAccess
import com.senspark.game.db.dailyMission.MissionDataAccess
import com.senspark.game.db.gachaChest.GachaChestDataAccess
import com.senspark.game.extension.modules.ServerServicesInitializer
import com.senspark.game.data.manager.autoMine.IAutoMineManager
import com.senspark.game.data.manager.treassureHunt.ITreasureHuntConfigManager
import com.senspark.game.db.dailyMission.IMissionDataAccess
import com.senspark.game.db.gachaChest.IGachaChestDataAccess
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.extension.coroutines.CoroutineScope
import com.senspark.game.extension.coroutines.ICoroutineScope
import com.senspark.game.manager.*
import com.senspark.game.manager.online.IUserOnlineManager
import com.senspark.game.manager.online.UserOnlineManager
import com.senspark.game.service.*
import com.senspark.game.user.ITrGameplayManager
import com.senspark.game.user.TrGameplayManager
import com.senspark.game.utils.*
import com.senspark.lib.data.manager.GameConfigManager
import com.senspark.lib.data.manager.IGameConfigManager
import com.senspark.lib.db.ILibDataAccess
import com.senspark.lib.db.LibDataAccessPostgreSql
import com.smartfoxserver.v2.extensions.SFSExtension

object MainGameExtensionModules {

    fun initModules(extension: SFSExtension): GlobalServices {
        val globalServices = GlobalServices("GLOBAL")
        createBaseServices(extension, globalServices)
        createSubServices(extension, globalServices)
        return globalServices
    }

    private fun createBaseServices(extension: SFSExtension, services: GlobalServices) {
        val envManager = PropertyEnvManager()
        HttpClient.initialize(envManager.logHttpRequest)
        val logAll = envManager.logAll
        val enableLogDb = envManager.logDb
        val logPvP = envManager.logPvp

        val logger = RemoteLogger(envManager.logRemoteData, null)
        val sqlLogger = MySqlLogger(logger, enableLogDb)
        val scheduler = SmartFoxScheduler(envManager.schedulerThreadSize, logger)
        val (cache, messenger) = RedisServices.create(envManager.redisConnectionString, SmartFoxScheduler(1, logger), logger)
        val gameConfigManager = GameConfigManager(logger)

        val database = DefaultDatabase(DatabaseUtils.create(envManager), sqlLogger)
        val shopDataAccess = ShopDataAccess(database, enableLogDb, logger)
        val gameDataAccess = GameDataAccessPostgreSql(database, enableLogDb, logger)
        val logDataAccess = LogDataAccessPostgreSql(database, enableLogDb, logger)
        val userDataAccess = CachedUserDataAccess(UserDataAccess(database, enableLogDb, logger), cache)
        val rewardDataAccess = RewardDataAccessPostgreSql(database, enableLogDb, logger)
        val libDataAccess = LibDataAccessPostgreSql(database, enableLogDb, logger)
        val pvpDataAccess = PvpDataAccess(
            database, logger, sqlLogger, logPvP, postgreSQLDatabaseStatement()
        )
        val gachaChestDataAccess = GachaChestDataAccess(database, enableLogDb, logger)
        val iapDataAccess = CachedIapDataAccess(IapDataAccess(database, enableLogDb, logger), cache)
        val pvpTournamentDataAccess = PvpTournamentDataAccess(database, enableLogDb, logger)
        val missionDataAccess = CachedMissionDataAccess(MissionDataAccess(database, enableLogDb, logger), cache)
        val thModeDataAccess = THModeDataAccess(database, enableLogDb, logger)
        val dataAccessManager = DataAccessManager(
            database,
            shopDataAccess,
            gameDataAccess,
            logDataAccess,
            userDataAccess,
            rewardDataAccess,
            libDataAccess,
            pvpDataAccess,
            gachaChestDataAccess,
            iapDataAccess,
            pvpTournamentDataAccess,
            missionDataAccess,
            thModeDataAccess,
            gameConfigManager
        )

        services.register(IEnvManager::class) { envManager }
        services.register(IGlobalLogger::class) { logger }
        services.register(IMessengerService::class) { messenger }
        services.register(ICacheService::class) { cache }
        services.register(IScheduler::class) { scheduler }
        services.register(IDataAccessManager::class) { dataAccessManager }

        services.register(IDatabase::class) { database }
        services.register(IShopDataAccess::class) { shopDataAccess }
        services.register(IGameDataAccess::class) { gameDataAccess }
        services.register(ILogDataAccess::class) { logDataAccess }
        services.register(IUserDataAccess::class) { userDataAccess }
        services.register(IRewardDataAccess::class) { rewardDataAccess }
        services.register(ILibDataAccess::class) { libDataAccess }
        services.register(IPvpDataAccess::class) { pvpDataAccess }
        services.register(IGachaChestDataAccess::class) { gachaChestDataAccess }
        services.register(IIapDataAccess::class) { iapDataAccess }
        services.register(IPvpTournamentDataAccess::class) { pvpTournamentDataAccess }
        services.register(IMissionDataAccess::class) { missionDataAccess }
        services.register(ITHModeDataAccess::class) { thModeDataAccess }
        services.register(IGameConfigManager::class) { gameConfigManager }
        services.register(ICoroutineScope::class) { CoroutineScope() }
        services.register(ITrGameplayManager::class) { TrGameplayManager() }
    }

    private fun createSubServices(
        extension: SFSExtension,
        globalServices: GlobalServices,
    ) {
        val g = globalServices
        val logger = g.get<IGlobalLogger>()

        g.register(ISender::class) { Sender(extension) }
        g.register(IHandlerLogger::class) {
            HandlerLogger(
                g.get<IScheduler>(),
                g.get<ICacheService>()
            )
        }
        g.register(IHeroAbilityConfigManager::class) { HeroAbilityConfigManager(g.get<IShopDataAccess>()) }
        g.register(IBlockDropByDayManager::class) { BlockDropByDayManager(g.get<IShopDataAccess>()) }
        g.register(IBlockRewardDataManager::class) { BlockRewardDataManager(g.get<IShopDataAccess>()) }
        g.register(IBlockConfigManager::class) { BlockConfigManager(g.get<IShopDataAccess>()) }
        g.register(IAutoMineManager::class) { AutoMineManager(g.get<IShopDataAccess>(), g.get<IGameDataAccess>()) }
        g.register(ITreasureHuntConfigManager::class) { TreasureHuntConfigManager(g.get<ITHModeDataAccess>()) }
        g.register(IUserOnlineManager::class) { UserOnlineManager(g.get<ICacheService>(), g.get<IGlobalLogger>()) }

        val svServices = ServerServicesInitializer.createServices(g, extension)
        
        g.register(ISvServicesContainer::class) { svServices }
        g.register(IAuthApi::class) { AuthApi(g.get<IEnvManager>(), svServices) }

        val msg = mutableListOf<String>()
        g.forEach { t ->
            val name = t.javaClass.simpleName
            t.initialize()
            msg.add(name)
        }
        logger.log("Initialized container ${g.name} services: ${msg.joinToString(", ")}")
    }
}
