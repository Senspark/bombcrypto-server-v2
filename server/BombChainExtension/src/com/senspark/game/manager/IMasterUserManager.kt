package com.senspark.game.manager

import com.senspark.game.controller.IUserHouseManager
import com.senspark.game.manager.ads.IUserBonusRewardManager
import com.senspark.game.manager.adventure.IUserAdventureModeManager
import com.senspark.game.manager.autoMine.UserAutoMineManager
import com.senspark.game.manager.blockMap.IUserBlockMapManager
import com.senspark.game.manager.blockReward.IUserBlockRewardManager
import com.senspark.game.manager.blockReward.IUserMiningModeManager
import com.senspark.game.manager.claim.IClaimManager
import com.senspark.game.manager.config.IUserConfigManager
import com.senspark.game.manager.costumePreset.IUserCostumePresetManager
import com.senspark.game.manager.dailyMission.IUserMissionManager
import com.senspark.game.manager.dailyTask.IUserDailyTaskManager
import com.senspark.game.manager.gachaChest.IUserGachaChestController
import com.senspark.game.manager.hero.IUserHeroFiManager
import com.senspark.game.manager.hero.IUserHeroTRManager
import com.senspark.game.manager.iap.IUserIAPShopManager
import com.senspark.game.manager.material.IUserMaterialManager
import com.senspark.game.manager.onBoarding.IUserOnBoardingManager
import com.senspark.game.manager.pvp.IUserBoosterManager
import com.senspark.game.manager.pvp.IUserPvpRankingManager
import com.senspark.game.manager.rock.IUserBuyRockManager
import com.senspark.game.manager.stake.IUserStakeManager
import com.senspark.game.manager.stake.UserStakeVipManager
import com.senspark.game.manager.subscription.IUserSubscriptionManager
import com.senspark.game.manager.ton.IUserDepositedTransactionManager
import com.senspark.game.manager.user.IUserDataManager
import com.senspark.game.manager.user.IUserOldItemManager
import com.senspark.game.user.IUserInventoryManager

interface IMasterUserManager {
    val heroTRManager: IUserHeroTRManager
    val heroFiManager: IUserHeroFiManager
    val houseManager: IUserHouseManager
    val userOldItemManager: IUserOldItemManager
    val userBlockMapManager: IUserBlockMapManager
    val blockRewardManager: IUserBlockRewardManager
    val userStakeManager: IUserStakeManager
    val userDepositedTransactionManager: IUserDepositedTransactionManager
    val userStakeVipManager: UserStakeVipManager
    val userPvPBoosterManager: IUserBoosterManager
    val userPvpRankingManager: IUserPvpRankingManager
    val claimManager: IClaimManager
    val userAutoMineManager: UserAutoMineManager
    val userBuyRockManager: IUserBuyRockManager
    val userMarketplaceManager: IUserMarketplaceManager
    val userInventoryManager: IUserInventoryManager
    val userDataManager: IUserDataManager
    val userMiningModeManager: IUserMiningModeManager
    val userAdventureModeManager: IUserAdventureModeManager
    val userConfigManager: IUserConfigManager
    val userGachaChestManager: IUserGachaChestController
    val userMissionManager: IUserMissionManager
    val userBonusRewardManager: IUserBonusRewardManager
    val userMaterialManager: IUserMaterialManager
    val userSubscriptionManager: IUserSubscriptionManager
    val userIAPShopManager: IUserIAPShopManager
    val userCostumePresetManager: IUserCostumePresetManager
    val userOnBoardingManager: IUserOnBoardingManager
    val userDailyTaskManager : IUserDailyTaskManager
    

    fun updateLogoutMediator()
}