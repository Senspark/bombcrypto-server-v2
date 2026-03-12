package com.senspark.game.manager

import com.senspark.game.api.IVerifyAdApiManager
import com.senspark.game.api.OkHttpRestApi
import com.senspark.game.api.subscription.SubscriptionApi
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.controller.UserHouseManager
import com.senspark.game.manager.ads.UserBonusRewardManager
import com.senspark.game.manager.adventure.UserAdventureModeManager
import com.senspark.game.manager.autoMine.UserAutoMineManager
import com.senspark.game.manager.blockMap.UserBlockMapManagerImpl
import com.senspark.game.manager.blockReward.UserBlockRewardManager
import com.senspark.game.manager.blockReward.UserMiningModeManager
import com.senspark.game.manager.claim.ClaimManagerPolygon
import com.senspark.game.manager.config.UserConfigManager
import com.senspark.game.manager.costumePreset.UserCostumePresetManager
import com.senspark.game.manager.dailyMission.UserMissionManager
import com.senspark.game.manager.dailyTask.UserDailyTaskManager
import com.senspark.game.manager.gachaChest.UserGachaChestController
import com.senspark.game.manager.hero.UserHeroFiManager
import com.senspark.game.manager.hero.UserHeroTRManager
import com.senspark.game.manager.iap.UserIAPShopManager
import com.senspark.game.manager.material.UserMaterialManager
import com.senspark.game.manager.onBoarding.UserOnBoardingManager
import com.senspark.game.manager.pvp.UserBoosterManager
import com.senspark.game.manager.pvp.UserPvpRankingManagerImpl
import com.senspark.game.manager.rock.UserBuyRockManager
import com.senspark.game.manager.stake.UserStakeManager
import com.senspark.game.manager.stake.UserStakeVipManagerImpl
import com.senspark.game.manager.subscription.UserSubscriptionManager
import com.senspark.game.manager.ton.UserDepositedTransactionManager
import com.senspark.game.manager.user.UserDataManager
import com.senspark.game.manager.user.UserOldItemManager
import com.senspark.game.user.IUserPermissions
import com.senspark.game.user.UserInventoryManager
import java.time.Instant

class MasterAirdropUserManager(
    userPermissions: IUserPermissions,
    private val _mediator: UserControllerMediator,
    verifyAdApi: IVerifyAdApiManager,
) : IMasterUserManager {
    private val api = OkHttpRestApi()

    override val houseManager = UserHouseManager(_mediator)
    override val userOldItemManager = UserOldItemManager(_mediator = _mediator)
    override val blockRewardManager = UserBlockRewardManager(_mediator)
    override val userSubscriptionManager = UserSubscriptionManager(_mediator, SubscriptionApi(_mediator, api))
    override val userConfigManager = UserConfigManager(_mediator, blockRewardManager) { userSubscriptionManager.noAds }
    override val userMissionManager = UserMissionManager(_mediator, verifyAdApi) { reloadUserReward() }
    override val userMaterialManager = UserMaterialManager(_mediator, blockRewardManager)
    override val heroTRManager =
        UserHeroTRManager(_mediator, blockRewardManager, userMaterialManager, userMissionManager)
    override val heroFiManager = UserHeroFiManager(_mediator, houseManager, blockRewardManager)
    override val userBlockMapManager = UserBlockMapManagerImpl(_mediator, blockRewardManager)
    override val userStakeManager = UserStakeManager(_mediator)
    override val userDepositedTransactionManager = UserDepositedTransactionManager(_mediator)
    override val userStakeVipManager = UserStakeVipManagerImpl(_mediator)
    override val userPvPBoosterManager = UserBoosterManager(_mediator, userOldItemManager)
    override val userPvpRankingManager = UserPvpRankingManagerImpl(_mediator)
    override val claimManager = ClaimManagerPolygon(_mediator, blockRewardManager)
    override val userAutoMineManager = UserAutoMineManager(_mediator)
    override val userBuyRockManager = UserBuyRockManager(_mediator)
    override val userMarketplaceManager = UserMarketplaceManager(
        _mediator,
        heroTRManager,
        userOldItemManager,
        userMaterialManager,
        blockRewardManager
    )
    override val userInventoryManager = UserInventoryManager(_mediator, blockRewardManager, heroTRManager, userMaterialManager)
    override val userDataManager = UserDataManager(_mediator, blockRewardManager)
    override val userMiningModeManager = UserMiningModeManager(_mediator)
    override val userGachaChestManager =
        UserGachaChestController(_mediator, userConfigManager, heroTRManager, blockRewardManager, verifyAdApi)
    override val userBonusRewardManager = UserBonusRewardManager(_mediator, verifyAdApi, blockRewardManager) {
        reloadUserReward()
    }
    override val userAdventureModeManager = UserAdventureModeManager(
        _mediator,
        heroTRManager,
        blockRewardManager,
        userGachaChestManager,
        userInventoryManager,
        userMissionManager,
        userPvPBoosterManager,
        userPermissions.storyOneHit,
        verifyAdApi,
        userSubscriptionManager,
        userBonusRewardManager,
    ) {
        reloadUserReward()
    }
    override val userIAPShopManager = UserIAPShopManager(_mediator, userSubscriptionManager, userConfigManager) {
        reloadUserReward()
    }
    override val userCostumePresetManager =
        UserCostumePresetManager(_mediator, userInventoryManager, heroTRManager, userConfigManager)
    override val userOnBoardingManager = UserOnBoardingManager(_mediator)
    override val userDailyTaskManager = UserDailyTaskManager(_mediator)

    init {
        houseManager.initHeroManager(heroFiManager)
        userSubscriptionManager.initUserConfigManager(userConfigManager)
    }

    override fun updateLogoutMediator() {
        _mediator.lastLogOut = { Instant.now() }
    }

    private fun reloadUserReward() {
        _mediator.saveGameAndLoadReward()
        heroTRManager.loadHero(true)
    }
}