package com.senspark.game.extension

import com.senspark.common.IDatabase
import com.senspark.common.cache.ICacheService
import com.senspark.common.cache.IMessengerService
import com.senspark.common.cache.RedisServices
import com.senspark.common.pvp.IRankManager
import com.senspark.common.service.IScheduler
import com.senspark.common.utils.IGlobalLogger
import com.senspark.common.utils.ILogger
import com.senspark.common.utils.MySqlLogger
import com.senspark.common.utils.RemoteLogger
import com.senspark.game.api.IInternalMessageHandler
import com.senspark.game.data.manager.season.IPvpSeasonManager
import com.senspark.game.data.manager.season.PvpSeasonManager
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.db.ShopDataAccess
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.extension.modules.ServerType
import com.senspark.game.extension.modules.SvServicesContainerContainer
import com.senspark.game.manager.DefaultDatabase
import com.senspark.game.manager.IPvpEnvManager
import com.senspark.game.manager.PvpEnvManager
import com.senspark.game.manager.pvp.MatchInfoUpdatedBroadcaster
import com.senspark.game.manager.pvp.PvpMatchManager
import com.senspark.game.pvp.PvpInternalMessageHandler
import com.senspark.game.pvp.manager.*
import com.senspark.game.service.IPvpDataAccess
import com.senspark.game.service.PvpDataAccess
import com.senspark.game.service.postgreSQLDatabaseStatement
import com.senspark.game.utils.*
import com.senspark.lib.data.manager.GameConfigManager
import com.senspark.lib.data.manager.IGameConfigManager
import com.senspark.lib.db.ILibDataAccess
import com.senspark.lib.db.LibDataAccessPostgreSql
import com.smartfoxserver.v2.api.CreateRoomSettings
import com.smartfoxserver.v2.entities.Room
import com.smartfoxserver.v2.extensions.SFSExtension
import java.util.concurrent.ScheduledThreadPoolExecutor

//import org.koin.core.context.startKoin
//import org.koin.core.module.Module
//import org.koin.dsl.module

object PvpZoneExtensionModules {
    fun initModules(extension: SFSExtension): PvpServices {
        val globalServices = GlobalServices("GLOBAL")
        val services = createServices(extension, globalServices)

//        startKoin {
//            modules(
//                initPvpModule(services),
//            )
//        }

        return services
    }

    private fun createServices(extension: SFSExtension, globalServices: GlobalServices): PvpServices {
        val parentZone = extension.parentZone
        val envManager = PvpEnvManager()
        val db = DatabaseUtils.create(
            envManager.postgresConnectionString,
            envManager.postgresDriverName,
            envManager.postgresUsername,
            envManager.postgresPassword,
            envManager.postgresMaxActiveConnections,
            envManager.postgresTestSql,
        )
        val logAll = false
        val logPvP = false
        val logDb = false

        val logger = RemoteLogger(envManager.logRemoteData, null)
        val sqlLogger = MySqlLogger(logger, logDb)
        val database = DefaultDatabase(db, sqlLogger)
        val sender = Sender(extension)

        val libDataAccess = LibDataAccessPostgreSql(database, logDb, logger)
        val gameConfigManager = GameConfigManager(logger)
        gameConfigManager.initialize(libDataAccess.loadGameConfig())

        val scheduler = SafeScheduler(logger, DefaultScheduler(logger, ScheduledThreadPoolExecutor(5)))
        val shopDataAccess = ShopDataAccess(database, logAll && logPvP, logger)
        val pvpDataAccess = PvpDataAccess(
            database,
            logger,
            sqlLogger,
            logAll,
            postgreSQLDatabaseStatement()
        )
        val defaultDatabaseManager = DefaultDatabaseManager(database, logAll && logPvP, logger)
        val pvpSeasonManager = PvpSeasonManager(shopDataAccess, gameConfigManager)
        val pvpRankManager = PvpRankManager(pvpDataAccess, pvpSeasonManager)
        val (cache, messenger) = RedisServices.create(
            envManager.redisConnectionString,
            scheduler,
            logger
        )
        val pvpInternalMessageHandler = PvpInternalMessageHandler(logger, parentZone)
        val pvpMatchManager = PvpMatchManager(
            defaultDatabaseManager,
            scheduler,
            sender,
            object : IRoomCreator {
                override fun createRoom(settings: CreateRoomSettings): Room {
                    return parentZone.createRoom(settings)
                }
            },
            EpochTimeManager(),
            MatchInfoUpdatedBroadcaster(messenger, parentZone),
            envManager,
            logger,
            messenger,
            cache,
            pvpDataAccess,
            pvpRankManager,
        )
        val serverInfoManager = ServerInfoManager(envManager, cache, logger)

        val serverServices = ServerServices(ServerType.BNB_POL.name)
        serverServices.register(IRankManager::class) { pvpRankManager }
        serverServices.register(IPvpSeasonManager::class) { pvpSeasonManager }

        val serverServicesContainer = SvServicesContainerContainer(
            mapOf(
                ServerType.BNB_POL to serverServices
            ), logger
        )

        globalServices.register(IPvpEnvManager::class) { envManager }
        globalServices.register(IGlobalLogger::class) { logger }
        globalServices.register(IScheduler::class) { scheduler }
        globalServices.register(IMessengerService::class) { messenger }
        globalServices.register(ICacheService::class) { cache }
        globalServices.register(IGameConfigManager::class) { gameConfigManager }
        globalServices.register(ILibDataAccess::class) { libDataAccess }
        globalServices.register(IShopDataAccess::class) { shopDataAccess }
        globalServices.register(IPvpDataAccess::class) { pvpDataAccess }
        globalServices.register(ISvServicesContainer::class) { serverServicesContainer }

        val msg = mutableListOf<String>()
        globalServices.forEach { t ->
            val name = t.javaClass.simpleName
            t.initialize()
            msg.add(name)
        }
        logger.log("Initialized container ${globalServices.name} services: ${msg.joinToString(", ")}")


        return PvpServices(
            envManager,
            logger,
            scheduler,
            database,
            libDataAccess,
            shopDataAccess,
            pvpDataAccess,
            defaultDatabaseManager,
            pvpRankManager,
            messenger,
            cache,
            pvpInternalMessageHandler,
            pvpMatchManager,
            serverInfoManager,
            gameConfigManager,
        )
    }

//    private fun initPvpModule(services: PvpServices): Module {
//        return module {
//            single<IPvpEnvManager> { services.envManager }
//            single<ILogger> { services.logger }
//            single<IScheduler> { services.scheduler }
//            single<IDatabase> { services.database }
//            single<ILibDataAccess> { services.libDataAccess }
//            single<IShopDataAccess> { services.shopDataAccess }
//            single<IPvpDataAccess> { services.pvpDataAccess }
//            single<IDatabaseManager> { services.databaseManager }
//            single<IRankManager> { services.rankManager }
//            single<IMessengerService> { services.messengerService }
//            single<ICacheService> { services.cacheService }
//            single<IInternalMessageHandler> { services.internalMessageHandler }
//            single<IMatchManager> { services.matchManager }
//            single<IServerInfoManager> { services.serverInfoManager }
//        }
//    }
}

data class PvpServices(
    val envManager: IPvpEnvManager,
    val logger: ILogger,
    val scheduler: IScheduler,
    val database: IDatabase,
    val libDataAccess: ILibDataAccess,
    val shopDataAccess: IShopDataAccess,
    val pvpDataAccess: IPvpDataAccess,
    val databaseManager: IDatabaseManager,
    val rankManager: IRankManager,
    val messengerService: IMessengerService,
    val cacheService: ICacheService,
    val internalMessageHandler: IInternalMessageHandler,
    val matchManager: IMatchManager,
    val serverInfoManager: IServerInfoManager,
    val gameConfigManager: GameConfigManager,
)