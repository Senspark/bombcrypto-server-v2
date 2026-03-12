package com.senspark.game.extension.modules

import com.senspark.common.cache.ICacheService
import com.senspark.common.cache.IMessengerService
import com.senspark.common.pvp.IRankManager
import com.senspark.common.service.IScheduler
import com.senspark.common.utils.IServerLogger
import com.senspark.common.utils.RemoteLogger
import com.senspark.game.api.*
import com.senspark.game.api.login.BasLoginManager
import com.senspark.game.api.login.ILoginManager
import com.senspark.game.data.manager.IMasterDataManager
import com.senspark.game.data.manager.MasterDataManager
import com.senspark.game.data.manager.adventure.*
import com.senspark.game.data.manager.dailyMission.IMissionManager
import com.senspark.game.data.manager.dailyMission.MissionManager
import com.senspark.game.data.manager.gacha.GachaChestSlotManager
import com.senspark.game.data.manager.gacha.IGachaChestSlotManager
import com.senspark.game.data.manager.grindHero.GrindHeroManager
import com.senspark.game.data.manager.grindHero.IGrindHeroManager
import com.senspark.game.data.manager.hero.*
import com.senspark.game.data.manager.iap.IAPShopManager
import com.senspark.game.data.manager.iap.IIAPShopManager
import com.senspark.game.data.manager.item.ConfigItemManager
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.manager.luckyWheel.ILuckyWheelRewardManager
import com.senspark.game.data.manager.luckyWheel.LuckyWheelRewardManager
import com.senspark.game.data.manager.mysteryBox.IMysteryBoxManager
import com.senspark.game.data.manager.mysteryBox.MysteryBoxManager
import com.senspark.game.data.manager.newUserGift.INewUserGiftManager
import com.senspark.game.data.manager.newUserGift.NewUserGiftManager
import com.senspark.game.data.manager.pvp.*
import com.senspark.game.data.manager.rock.BuyRockManager
import com.senspark.game.data.manager.rock.IBuyRockManager
import com.senspark.game.data.manager.season.IPvpSeasonManager
import com.senspark.game.data.manager.season.PvpSeasonManager
import com.senspark.game.data.manager.stake.IStakeVipRewardManager
import com.senspark.game.data.manager.stake.StakeVipRewardManager
import com.senspark.game.data.manager.subscription.ISubscriptionManager
import com.senspark.game.data.manager.subscription.SubscriptionManager
import com.senspark.game.data.manager.treassureHunt.CoinRankingManager
import com.senspark.game.data.manager.treassureHunt.HouseManager
import com.senspark.game.data.manager.treassureHunt.ICoinRankingManager
import com.senspark.game.data.manager.treassureHunt.IHouseManager
import com.senspark.game.data.manager.upgradeHero.IUpgradeCrystalManager
import com.senspark.game.data.manager.upgradeHero.IUpgradeHeroTrManager
import com.senspark.game.data.manager.upgradeHero.UpgradeCrystalManager
import com.senspark.game.data.manager.upgradeHero.UpgradeHeroTrManager
import com.senspark.game.db.*
import com.senspark.game.db.dailyMission.IMissionDataAccess
import com.senspark.game.db.gachaChest.IGachaChestDataAccess
import com.senspark.game.declare.EnumConstants
import com.senspark.game.extension.GlobalServices
import com.senspark.game.extension.ServerServices
import com.senspark.game.extension.events.IUserTournamentWhitelistManager
import com.senspark.game.extension.events.UserTournamentWhitelistManager
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IUsersManager
import com.senspark.game.manager.UsersManager
import com.senspark.game.manager.convertToken.ISwapTokenRealtimeManager
import com.senspark.game.manager.convertToken.SwapTokenRealtimeManager
import com.senspark.game.manager.dailyTask.DailyTaskManager
import com.senspark.game.manager.dailyTask.IDailyTaskManager
import com.senspark.game.manager.market.IMarketManager
import com.senspark.game.manager.market.MarketManager
import com.senspark.game.manager.offlineReward.IOfflineRewardManager
import com.senspark.game.manager.offlineReward.OfflineRewardManager
import com.senspark.game.manager.resourceSync.ISyncResourceManager
import com.senspark.game.manager.resourceSync.SyncResourceManager
import com.senspark.game.manager.rock.IUserRockManager
import com.senspark.game.manager.rock.UserRockManager
import com.senspark.game.manager.stake.HeroStakeManager
import com.senspark.game.manager.stake.IHeroStakeManager
import com.senspark.game.manager.ton.*
import com.senspark.game.manager.treasureHuntV2.ITreasureHuntV2Manager
import com.senspark.game.manager.treasureHuntV2.NullTreasureHuntV2Manager
import com.senspark.game.manager.user.IUserLinkManager
import com.senspark.game.manager.user.UserLinkManager
import com.senspark.game.pvp.IPvpResultManager
import com.senspark.game.pvp.PvpResultManager
import com.senspark.game.pvp.manager.IPvpQueueManager
import com.senspark.game.pvp.manager.PvpQueueManager
import com.senspark.game.pvp.manager.PvpRankManager
import com.senspark.game.service.*
import com.senspark.game.user.GachaChestManager
import com.senspark.game.user.IGachaChestManager
import com.senspark.game.user.ITrGameplayManager
import com.senspark.game.utils.ISender
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.extensions.SFSExtension

class ServicesInitializerBas(
    private val _globalServices: GlobalServices,
    private val _extension: SFSExtension?,
) : IPerServerServicesInitializer {
    override val serverType = ServerType.BAS

    override fun createService(): ServerServices {
        val g = _globalServices
        val n = ServerServices(serverType.name)
        val env = g.get<IEnvManager>()
        val gameConfig = g.get<IGameConfigManager>()
        val http = OkHttpRestApi()
        val logger = RemoteLogger(env.logRemoteData, "bas")
        
        n.register(ICoinRankingManager::class) {
            CoinRankingManager(
                g.get<ITHModeDataAccess>(),
                g.get<IShopDataAccess>(),
                gameConfig,
                EnumConstants.DataType.BAS
            )
        }
        n.register(ILoginManager::class) {
            BasLoginManager(
                g.get<IAuthApi>(),
                g.get<IUserDataAccess>(),
            )
        }
        n.register(IServerLogger::class) { logger }
        n.register(IHouseManager::class) { HouseManager(g.get<IShopDataAccess>()) }
        n.register(IClubManager::class) { NullClubManager() }
        n.register(IReferralManager::class) { NullReferralManager() }
        n.register(ITasksManager::class) { NullTasksManager() }
        n.register(IBlockchainDatabaseManager::class) { NullBlockchainDatabaseManager() }
        n.register(IConfigItemManager::class) { ConfigItemManager(g.get<IShopDataAccess>()) }
        n.register(IPvpConfigManager::class) { PvpConfigManager(env, logger) }
        n.register(IUserLinkManager::class) { UserLinkManager() }
        n.register(IHeroUpgradeShieldManager::class) { NullHeroUpgradeShieldManager() }
        n.register(IUserTournamentWhitelistManager::class) {
            UserTournamentWhitelistManager(
                g.get<IPvpTournamentDataAccess>(),
                env,
                g.get<ICacheService>(),
                logger
            )
        }
        n.register(ISwapTokenRealtimeManager::class) {
            SwapTokenRealtimeManager(
                env,
                g.get<IGameDataAccess>(),
                g.get<IShopDataAccess>(),
                g.get<IPvpDataAccess>(),
                g.get<ICacheService>(),
                logger
            )
        }
        n.register(IHeroStakeManager::class) {
            HeroStakeManager(
                env,
                g.get<IGameDataAccess>(),
                g.get<IShopDataAccess>()
            )
        }
        n.register(IAllHeroesFiManager::class) { AllHeroesFiManager(logger, g.get<IGameDataAccess>()) }
        n.register(IHeroUpgradePowerManager::class) { HeroUpgradePowerManager(g.get<IShopDataAccess>(), logger) }
        n.register(IConfigHeroTraditionalManager::class) {
            ConfigHeroTraditionalManager(
                g.get<IShopDataAccess>(),
                n.get<IHeroBuilder>()
            )
        }
        n.register(IPvpQueueManager::class) {
            PvpQueueManager(
                logger,
                env,
                g.get<IScheduler>(),
                g.get<IMessengerService>(),
                g.get<ICacheService>(),
                n.get<IConfigHeroTraditionalManager>(),
                g.get<ISender>(),
                n.get<IUsersManager>()
            )
        }
        n.register(IGachaChestManager::class) {
            GachaChestManager(
                g.get<IShopDataAccess>(),
                n.get<IConfigItemManager>()
            )
        }
        n.register(IGachaChestSlotManager::class) { GachaChestSlotManager(g.get<IShopDataAccess>()) }
        n.register(IPvpSeasonManager::class) { PvpSeasonManager(g.get<IShopDataAccess>(), g.get<IGameConfigManager>()) }
        n.register(IMissionManager::class) { MissionManager(g.get<IShopDataAccess>(), g.get<IMissionDataAccess>()) }
        n.register(IPvpResultManager::class) {
            PvpResultManager(
                env,
                g.get<IUserDataAccess>(),
                g.get<IGachaChestDataAccess>(),
                n.get<IGachaChestManager>(),
                n.get<IGachaChestSlotManager>(),
                n.get<IPvpQueueManager>(),
                g.get<IPvpDataAccess>(),
                n.get<IPvpSeasonManager>(),
                n.get<IMissionManager>(),
                n.get<IUsersManager>(),
                g.get<ITrGameplayManager>(),
                n.get<IPvpRankingManager>(),
            )
        }
        n.register(IGameFeatureConfigManager::class) { GameFeatureConfigManager(g.get<IGameDataAccess>()) }
        n.register(IRankManager::class) { PvpRankManager(g.get<IPvpDataAccess>(), n.get<IPvpSeasonManager>()) }
        n.register(IMasterDataManager::class) {
            MasterDataManager(
                n.get<IConfigItemManager>(),
                n.get<IConfigHeroTraditionalManager>(),
                n.get<IGameFeatureConfigManager>(),
                n.get<IRankManager>(),
                n.get<IPvpSeasonManager>(),
                g.get<IUserDataAccess>(),
                gameConfig
            )
        }
        n.register(IResetShieldBomberManager::class) { ResetShieldBomberManager(g.get<IShopDataAccess>()) }
        n.register(IHeroRepairShieldDataManager::class) { HeroRepairShieldDataManager(g.get<IShopDataAccess>()) }
        n.register(IAdventureItemManager::class) { AdventureItemManager(g.get<IShopDataAccess>()) }
        n.register(IAdventureReviveHeroCostManager::class) { AdventureReviveHeroCostManager(g.get<IShopDataAccess>()) }
        n.register(IAdventureEnemyConfigManager::class) { AdventureEnemyConfigManager(g.get<IShopDataAccess>()) }
        n.register(IAdventureLevelConfigManager::class) { AdventureLevelConfigManager(g.get<IShopDataAccess>()) }
        n.register(IStakeVipRewardManager::class) { StakeVipRewardManager(g.get<IShopDataAccess>()) }
        n.register(IMysteryBoxManager::class) {
            MysteryBoxManager(
                n.get<IConfigItemManager>(),
                g.get<IShopDataAccess>()
            )
        }
        n.register(ILuckyWheelRewardManager::class) {
            LuckyWheelRewardManager(
                g.get<IShopDataAccess>(),
                n.get<IConfigItemManager>(),
                n.get<IMysteryBoxManager>()
            )
        }
        n.register(INewUserGiftManager::class) {
            NewUserGiftManager(
                g.get<IShopDataAccess>(),
                g.get<IRewardDataAccess>(),
                n.get<IConfigItemManager>(),
                n.get<IConfigHeroTraditionalManager>(),
                gameConfig
            )
        }
        n.register(IGrindHeroManager::class) { GrindHeroManager(g.get<IShopDataAccess>(), n.get<IConfigItemManager>()) }
        n.register(IUpgradeCrystalManager::class) { UpgradeCrystalManager(g.get<IShopDataAccess>()) }
        n.register(IUpgradeHeroTrManager::class) { UpgradeHeroTrManager(g.get<IShopDataAccess>()) }
        n.register(ISubscriptionManager::class) { SubscriptionManager(g.get<IShopDataAccess>()) }
        n.register(IVerifyAdApiManager::class) { VerifyAdApiManager(env, logger) }
        n.register(IOfflineRewardManager::class) {
            OfflineRewardManager(
                g.get<IRewardDataAccess>(),
                g.get<IGameDataAccess>(),
                n.get<IConfigItemManager>(),
                n.get<IConfigHeroTraditionalManager>(),
                n.get<IVerifyAdApiManager>()
            )
        }
        n.register(IIAPShopManager::class) {
            IAPShopManager(
                g.get<IShopDataAccess>(),
                env,
                n.get<IVerifyAdApiManager>(),
                http,
                logger,
            )
        }
        n.register(IPvpRankingRewardManager::class) {
            PvpRankingRewardManager(
                g.get<IUserDataAccess>(),
                gameConfig.minPvpMatchCountToGetReward
            )
        }
        n.register(IPvpTournamentManager::class) {
            PvpTournamentManager(
                g.get<IShopDataAccess>(),
                n.get<IRankManager>(),
                n.get<IPvpSeasonManager>()
            )
        }
        n.register(IBuyRockManager::class) { BuyRockManager(g.get<IShopDataAccess>()) }
        n.register(IUserRockManager::class) {
            UserRockManager(
                g.get<IShopDataAccess>(),
                g.get<IRewardDataAccess>(),
                g.get<IGameDataAccess>(),
                env,
                n.get<IHeroUpgradeShieldManager>(),
                n.get<IHeroBuilder>()
            )
        }
        n.register(ITreasureHuntV2Manager::class) { NullTreasureHuntV2Manager() }
        n.register(IPvpRankingManager::class) {
            PvpRankingManager(
                logger,
                env,
                g.get<IPvpDataAccess>(),
                g.get<IUserDataAccess>(),
                n.get<IPvpSeasonManager>(),
                n.get<IRankManager>(),
                n.get<IUsersManager>(),
                gameConfig,
            )
        }
        n.register(IServerInfoManager::class) { ServerInfoManager(g.get<ICacheService>(), env, logger, gameConfig) }
        n.register(IHeroBuilder::class) {
            HeroBuilder(
                g.get<IGameDataAccess>(),
                n.get<IHeroStakeManager>(),
                g.get<IHeroAbilityConfigManager>(),
                n.get<IHeroUpgradePowerManager>(),
                n.get<IHeroUpgradeShieldManager>()
            )
        }
        n.register(IUsersManager::class) { UsersManager(logger) }
        n.register(IForceLoginManager::class) { ForceLoginManager(n.get<IUsersManager>(), logger) }
        n.register(IDailyTaskManager::class) {
            DailyTaskManager(
                g.get<IUserDataAccess>(),
                g.get<ICacheService>(),
                logger,
                g.get<IGameConfigManager>()
            )
        }
        n.register(IMarketApi::class) {
            MarketApi(
                g.get<IEnvManager>(),
                logger
            )
        }
        n.register(IMarketManager::class) {
            MarketManager(
                g.get<ICacheService>(),
                n.get<IMarketApi>(),
                logger
            )
        }
        n.register(ISyncResourceManager::class) {
            SyncResourceManager(
                g.get<IDataAccessManager>(),
                g.get<IGameDataAccess>(),
                g.get<IUserDataAccess>(),
                g.get<IGameConfigManager>(),

                n.get<IHeroStakeManager>(),
                n.get<IHeroBuilder>(),
                n.get<IAllHeroesFiManager>(),
                n.get<IConfigHeroTraditionalManager>()
            )
        }
        
        return n
    }
}
