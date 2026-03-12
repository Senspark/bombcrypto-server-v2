package com.senspark.game.db

import com.senspark.common.constant.PvPItemType
import com.senspark.common.pvp.IPvpFixtureMatchInfo
import com.senspark.common.pvp.IRankManager
import com.senspark.common.service.IGlobalService
import com.senspark.game.constant.ItemKind
import com.senspark.game.controller.IUserController
import com.senspark.game.data.HeroUpgradeShieldData
import com.senspark.game.data.manager.iap.FreeRewardConfigItem
import com.senspark.game.data.manager.iap.IAPGoldShopConfigItem
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.model.config.*
import com.senspark.game.data.model.nft.ConfigHeroTraditional
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.customEnum.ConfigUpgradeHeroType
import com.senspark.game.declare.customEnum.GachaChestType
import com.senspark.game.declare.customEnum.IAPShopType
import com.senspark.game.declare.customEnum.SubscriptionProduct
import com.senspark.game.declare.customTypeAlias.ProductId
import com.senspark.game.manager.blockReward.IUserBlockRewardManager
import com.senspark.game.manager.rock.RockAmount
import java.time.Instant

interface IShopDataAccess : IGlobalService {
    fun loadBlock(): HashMap<EnumConstants.DataType, HashMap<Int, BlockConfig>>
    fun loadBlockReward(): HashMap<EnumConstants.DataType, HashMap<Int, MutableList<IBlockReward>>>
    fun loadBlockDropByDay(): HashMap<EnumConstants.DataType, List<BlockDropRate>>
    fun loadAdventureLevelStrategy(): Map<Int, Map<Int, LevelStrategy>>
    fun loadDailyMission(): Map<String, Mission>
    fun loadAdventureEnemyCreator(): Map<Int, EnemyCreator>
    fun loadReviveHeroCosts(): Map<Int, ReviveHeroCost>
    fun loadGachaChestSlots(): Map<Int, GachaChestSlot>
    fun loadBomberAbility(): Map<Int, HeroAbilityConfig>
    fun loadHeroUpgradePower(): Map<Int, HeroUpgradePower>
    fun loadStakeVipRewards(): Map<Int, List<StakeVipReward>>
    fun loadResetShieldBomber(): Map<Int, ResetShieldBomber>
    fun loadRankingSeason(): MutableMap<Int, Season>
    fun addNewPVPRankingSeason(newSeasonId: Int, timeStartNextSeason: Instant, timeEndNextSeason: Instant)
    fun loadCoinRankingSeason(): MutableMap<Int, Season>
    fun queryHeroUpgradeShield(): List<HeroUpgradeShieldData>
    fun loadHeroRepairShield(): Map<Int, Map<Int, HeroRepairShield>>
    fun getHeroTraditionalConfigs(): Map<Int, ConfigHeroTraditional>
    fun getIAPGemShopConfigs(): Map<IAPShopType, Map<ProductId, IAPShopConfig>>
    fun getIAPGoldShopConfigs(): List<IAPGoldShopConfigItem>
    fun getFreeRewardConfigs(): List<FreeRewardConfigItem>
    fun buyGoldByGem(
        userInfo: IUserInfo,
        userBlockRewardManager: IUserBlockRewardManager,
        configItem: IAPGoldShopConfigItem
    )

    fun loadAdventureItem(): Map<PvPItemType, AdventureItem>
    fun getConfigItem(): Map<Int, Item>
    fun loadLuckyWheelReward(configItemManager: IConfigItemManager): List<LuckyWheelReward>
    fun addFreeReward(
        userController: IUserController,
        freeRewardConfigItem: FreeRewardConfigItem,
        reason: String
    )

    fun loadMysteryBox(configItemManager: IConfigItemManager): List<MysteryBox>
    fun loadNewUserGift(configItemManager: IConfigItemManager): List<NewUserGift>
    fun loadGrindHeroConfig(configItemManager: IConfigItemManager): Map<ItemKind, GrindHero>
    fun loadUpgradeCrystalConfig(): Map<Int, UpgradeCrystal>
    fun loadUpgradeHeroTrConfig(): Map<ConfigUpgradeHeroType, Map<Int, UpgradeHeroTr>>
    fun loadSubscription(): MutableMap<SubscriptionProduct, SubscriptionPackage>
    fun loadPvpFixture(rankManager: IRankManager, currentSeasonNumber: Int): List<IPvpFixtureMatchInfo>
    fun loadGachaChestConfigs(configItemManager: IConfigItemManager): Map<GachaChestType, IGachaChest>
    fun loadCostumePresetPrice(): Map<BLOCK_REWARD_TYPE, Int>
    fun loadMinStakeHeroConfig(): Map<Int, Int>
    fun loadSwapTokenConfig(): List<SwapTokenConfig>
    fun loadSwapTokenRealtimeConfig(): SwapTokenRealtimeConfig
    fun loadAutoMinePackageConfig(): Map<EnumConstants.DataType, List<AutoMinePackage>>
    fun loadBurnHeroConfig(): Map<Int, RockAmount>
    fun loadRockPackageConfig(): List<RockPackage>
    fun loadHouseRentPackageConfig(): Map<EnumConstants.DataType, List<HouseRentPackage>>
}