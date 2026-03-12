package com.senspark.game.extension.modules

import com.senspark.common.pvp.IRankManager
import com.senspark.common.utils.IServerLogger
import com.senspark.common.utils.RemoteLogger
import com.senspark.game.api.*
import com.senspark.game.api.login.ILoginManager
import com.senspark.game.api.login.SolLoginManager
import com.senspark.game.data.manager.IMasterDataManager
import com.senspark.game.data.manager.NullMasterDataManager
import com.senspark.game.data.manager.adventure.*
import com.senspark.game.data.manager.dailyMission.IMissionManager
import com.senspark.game.data.manager.dailyMission.NullMissionManager
import com.senspark.game.data.manager.gacha.IGachaChestSlotManager
import com.senspark.game.data.manager.gacha.NullGachaChestSlotManager
import com.senspark.game.data.manager.grindHero.IGrindHeroManager
import com.senspark.game.data.manager.grindHero.NullGrindHeroManager
import com.senspark.game.data.manager.hero.*
import com.senspark.game.data.manager.iap.IIAPShopManager
import com.senspark.game.data.manager.iap.NullIAPShopManager
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.manager.item.NullConfigItemManager
import com.senspark.game.data.manager.luckyWheel.ILuckyWheelRewardManager
import com.senspark.game.data.manager.luckyWheel.NullLuckyWheelRewardManager
import com.senspark.game.data.manager.mysteryBox.IMysteryBoxManager
import com.senspark.game.data.manager.mysteryBox.NullMysteryBoxManager
import com.senspark.game.data.manager.newUserGift.INewUserGiftManager
import com.senspark.game.data.manager.newUserGift.NullNewUserGiftManager
import com.senspark.game.data.manager.pvp.*
import com.senspark.game.data.manager.rock.IBuyRockManager
import com.senspark.game.data.manager.rock.NullBuyRockManager
import com.senspark.game.data.manager.season.IPvpSeasonManager
import com.senspark.game.data.manager.season.NullPvpSeasonManager
import com.senspark.game.data.manager.stake.IStakeVipRewardManager
import com.senspark.game.data.manager.stake.NullStakeVipRewardManager
import com.senspark.game.data.manager.subscription.ISubscriptionManager
import com.senspark.game.data.manager.subscription.NullSubscriptionManager
import com.senspark.game.data.manager.treassureHunt.CoinRankingManager
import com.senspark.game.data.manager.treassureHunt.HouseManager
import com.senspark.game.data.manager.treassureHunt.ICoinRankingManager
import com.senspark.game.data.manager.treassureHunt.IHouseManager
import com.senspark.game.data.manager.upgradeHero.IUpgradeCrystalManager
import com.senspark.game.data.manager.upgradeHero.IUpgradeHeroTrManager
import com.senspark.game.data.manager.upgradeHero.NullUpgradeCrystalManager
import com.senspark.game.data.manager.upgradeHero.NullUpgradeHeroTrManager
import com.senspark.game.db.*
import com.senspark.game.declare.EnumConstants
import com.senspark.game.extension.GlobalServices
import com.senspark.game.extension.ServerServices
import com.senspark.game.extension.events.IUserTournamentWhitelistManager
import com.senspark.game.extension.events.NullUserTournamentWhitelistManager
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IUsersManager
import com.senspark.game.manager.UsersManager
import com.senspark.game.manager.convertToken.ISwapTokenRealtimeManager
import com.senspark.game.manager.convertToken.NullSwapTokenRealtimeManager
import com.senspark.game.manager.offlineReward.IOfflineRewardManager
import com.senspark.game.manager.offlineReward.NullOfflineRewardManager
import com.senspark.game.manager.resourceSync.ISyncResourceManager
import com.senspark.game.manager.resourceSync.NullSyncResourceManager
import com.senspark.game.manager.rock.IUserRockManager
import com.senspark.game.manager.rock.NullUserRockManager
import com.senspark.game.manager.stake.IHeroStakeManager
import com.senspark.game.manager.stake.NullHeroStakeManager
import com.senspark.game.manager.ton.*
import com.senspark.game.manager.treasureHuntV2.ITreasureHuntV2Manager
import com.senspark.game.manager.treasureHuntV2.NullTreasureHuntV2Manager
import com.senspark.game.manager.user.IUserLinkManager
import com.senspark.game.manager.user.NullUserLinkManager
import com.senspark.game.pvp.IPvpResultManager
import com.senspark.game.pvp.NullPvpResultManager
import com.senspark.game.pvp.manager.IPvpQueueManager
import com.senspark.game.pvp.manager.NullPvpQueueManager
import com.senspark.game.pvp.manager.NullPvpRankManager
import com.senspark.game.service.*
import com.senspark.game.user.IGachaChestManager
import com.senspark.game.user.NullGachaChestManager
import com.senspark.lib.data.manager.IGameConfigManager

class ServicesInitializerSol(
    private val _globalServices: GlobalServices,
) : IPerServerServicesInitializer {
    override val serverType = ServerType.SOL

    override fun createService(): ServerServices {
        val g = _globalServices
        val env = g.get<IEnvManager>()
        val gameConfig = g.get<IGameConfigManager>()
        
        val n = ServerServices(serverType.name)
        val logger = RemoteLogger(env.logRemoteData, "sol")

        n.register(IServerLogger::class) { logger }
        n.register(ICoinRankingManager::class) {
            CoinRankingManager(g.get<ITHModeDataAccess>(), g.get<IShopDataAccess>(),gameConfig, EnumConstants.DataType.SOL)
        }
        n.register(IHouseManager::class) { HouseManager(g.get<IShopDataAccess>()) }
        n.register(IClubManager::class) { NullClubManager() }
        n.register(IReferralManager::class) { NullReferralManager() }
        n.register(ITasksManager::class) { NullTasksManager() }
        n.register(IBlockchainDatabaseManager::class) { NullBlockchainDatabaseManager() }
        n.register(IConfigItemManager::class) { NullConfigItemManager() }
        n.register(IPvpConfigManager::class) { NullPvpConfigManager() }
        n.register(IUserLinkManager::class) { NullUserLinkManager() }
        n.register(IHeroUpgradeShieldManager::class) { NullHeroUpgradeShieldManager() }
        n.register(IUserTournamentWhitelistManager::class) { NullUserTournamentWhitelistManager() }
        n.register(ISwapTokenRealtimeManager::class) { NullSwapTokenRealtimeManager() }
        n.register(IHeroStakeManager::class) { NullHeroStakeManager() }
        n.register(IAllHeroesFiManager::class) { NullAllHeroesFiManager() }
        n.register(IHeroUpgradePowerManager::class) { NullHeroUpgradePowerManager() }
        n.register(IConfigHeroTraditionalManager::class) { NullConfigHeroTraditionalManager() }
        n.register(IPvpQueueManager::class) { NullPvpQueueManager() }
        n.register(IGachaChestManager::class) { NullGachaChestManager() }
        n.register(IGachaChestSlotManager::class) { NullGachaChestSlotManager() }
        n.register(IPvpSeasonManager::class) { NullPvpSeasonManager() }
        n.register(IMissionManager::class) { NullMissionManager() }
        n.register(IPvpResultManager::class) { NullPvpResultManager() }
        n.register(IGameFeatureConfigManager::class) { NullGameFeatureConfigManager() }
        n.register(IRankManager::class) { NullPvpRankManager() }
        n.register(IMasterDataManager::class) { NullMasterDataManager() }
        n.register(IResetShieldBomberManager::class) { NullResetShieldBomberManager() }
        n.register(IHeroRepairShieldDataManager::class) { NullHeroRepairShieldDataManager() }
        n.register(IAdventureItemManager::class) { NullAdventureItemManager() }
        n.register(IAdventureReviveHeroCostManager::class) { NullAdventureReviveHeroCostManager() }
        n.register(IAdventureEnemyConfigManager::class) { NullAdventureEnemyConfigManager() }
        n.register(IAdventureLevelConfigManager::class) { NullAdventureLevelConfigManager() }
        n.register(IStakeVipRewardManager::class) { NullStakeVipRewardManager() }
        n.register(IMysteryBoxManager::class) { NullMysteryBoxManager() }
        n.register(ILuckyWheelRewardManager::class) { NullLuckyWheelRewardManager() }
        n.register(INewUserGiftManager::class) { NullNewUserGiftManager() }
        n.register(IGrindHeroManager::class) { NullGrindHeroManager() }
        n.register(IUpgradeCrystalManager::class) { NullUpgradeCrystalManager() }
        n.register(IUpgradeHeroTrManager::class) { NullUpgradeHeroTrManager() }
        n.register(ISubscriptionManager::class) { NullSubscriptionManager() }
        n.register(IVerifyAdApiManager::class) { NullVerifyAdApiManager() }
        n.register(IOfflineRewardManager::class) { NullOfflineRewardManager() }
        n.register(IIAPShopManager::class) { NullIAPShopManager() }
        n.register(IPvpRankingRewardManager::class) { NullPvpRankingRewardManager() }
        n.register(IPvpTournamentManager::class) { NullPvpTournamentManager() }
        n.register(IBuyRockManager::class) { NullBuyRockManager() }
        n.register(IUserRockManager::class) { NullUserRockManager() }
        n.register(ITreasureHuntV2Manager::class) { NullTreasureHuntV2Manager() }
        n.register(IServerInfoManager::class) { NullServerInfoManager() }
        n.register(ILoginManager::class) { SolLoginManager(g.get<IAuthApi>(), g.get<IUserDataAccess>()) }
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
        n.register(IPvpQueueManager::class) { NullPvpQueueManager() }
        n.register(ISyncResourceManager::class) { NullSyncResourceManager() }

        return n
    }
}