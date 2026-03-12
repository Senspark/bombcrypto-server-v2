package com.senspark.game.db

import com.senspark.common.constant.ItemId
import com.senspark.game.constant.ItemStatus
import com.senspark.game.data.manager.gacha.IGachaChestSlotManager
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.model.Filter
import com.senspark.game.data.model.PvpRankingReward
import com.senspark.game.data.model.auth.IUserLoginInfo
import com.senspark.game.data.model.autoMine.IUserAutoMine
import com.senspark.game.data.model.config.*
import com.senspark.game.data.model.deposit.UserDeposited
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.user.*
import com.senspark.game.db.model.UserConfig
import com.senspark.game.declare.EnumConstants.*
import com.senspark.game.declare.customEnum.SubscriptionProduct
import com.senspark.game.declare.customEnum.SubscriptionState
import com.senspark.game.manager.config.MiscConfigs
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.google.gson.JsonArray
import com.senspark.common.service.IGlobalService
import com.senspark.game.api.IPvpResultInfo
import com.senspark.game.manager.dailyTask.DailyTask
import com.senspark.game.manager.dailyTask.TodayTask
import com.senspark.game.manager.dailyTask.UserTask
import com.senspark.game.manager.onBoarding.UserProgress

interface IUserDataAccess : IGlobalService {
    fun getUserInfo(info: IUserLoginInfo, deviceType: DeviceType): IUserInfo
    fun getDisplayNameUser(uid: Int): String
    fun saveUserLoginInfo(info: IUserLoginInfo, deviceType: DeviceType): IUserInfo

    fun changeMiningToken(uid: Int, tokenType: TokenType): Boolean

    @Throws(Exception::class)
    fun loadUserPvpBoosters(uid: Int): Map<Int, UserBooster>

    fun changeDefaultMiningToken(uid: Int, tokenType: TokenType): Boolean
    fun getConfigPvpRankingReward(): List<PvpRankingReward>
    fun loadUserStakeVip(username: String): List<UserStakeVipReward>
    fun claimStakeVipReward(uid: Int, dataType: DataType, userStakeVipReward: UserStakeVipReward)

    fun subUserPvpBoosters(uid: Int, boosterIds: List<Int>)
    fun summaryPvpRankingReward()
    fun isSummaryStoryHunterSeason(): Boolean
    fun buyItemMarketplace(
        item: Item,
        quantity: Int,
        unitPrice: Float,
        buyerId: Int,
        rewardType: BLOCK_REWARD_TYPE,
        expirationAfter: Int
    )

    fun sellItemMarketplace(
        listId: List<Int>,
        type: Int,
        itemId: Int,
        quantity: Int,
        price: Float,
        uid: Int,
        rewardType: Int,
        expirationAfter: Int
    )

    fun cancelItemMarketplace(uid: Int, itemId: Int, quantity: Int, unitPrice: Float, expirationAfter: Int)
    fun editItemMarketplace(
        uid: Int,
        itemId: Int,
        oldQuantity: Int,
        oldUnitPrice: Float,
        newQuantity: Int,
        newUnitPrice: Float,
        expirationAfter: Int
    )

    fun getPvpRankingReward(
        pvpMatchReward: Int,
        season: Int
    ): Map<Int, UserPvpRankingReward>

    fun getStoryHunterBuyTicket(season: Int): Int
    fun isLoadDataManager(schedule: String, season: Int): Boolean
    fun logRepairShield(username: String, dataType: DataType, repairedBomberman: Map<Int, Int>)
    fun updateAccountName(
        dataType: DataType,
        username: String,
        newName: String,
        fee: Float,
        deposit: Float,
        feeRewardType: BLOCK_REWARD_TYPE
    )

    fun getNextRoundStoryHunter(userName: String): Int
    fun logPvpBooster(type: String, itemId: Int, boosterName: String, userId: Int, boosterPrice: Int)
    fun hasClaimReward(userId: Int, season: Int): Boolean
    fun insertLogRewardDeposit(userName: String, rewardType: String, rewardAmount: Float)
    fun getUserInventory(
        uid: Int,
        filter: Filter = Filter.all(),
        status: Int = ItemStatus.Sell.value
    ): MutableMap<ItemId, MutableList<UserItem>>

    fun getUserBooster(
        uid: Int,
        filter: Filter = Filter.all(),
    ): Map<Int, List<UserItem>>

    fun getMarketplaceItem(id: Int, configProductManager: IConfigItemManager): MarketplaceItem
    fun getTotalCountItemInMarket(excludeOwnerId: Int, filter: Filter): Int

    fun getActivity(uid: Int): Map<Int, Activity>
    fun activateCode(uid: Int, code: String): Boolean

    fun buyAutoMinePackage(
        uid: Int,
        autoMinePackage: AutoMinePackage,
        dataType: DataType,
        firstRewardType: String,
        secondRewardType: String
    )
    fun buyRockPackage(
        uid: Int,
        packageName: String,
        network: String,
        dataType: String,
        secondDataType: String
    )
    
    fun loadUserAutoMinePackage(uid: Int, dataType: DataType): IUserAutoMine
    fun loadAutoMinePackagePrice(uid: Int, listArrayPackage: JsonArray): ISFSArray
    
    fun resetShieldHero(
        dataType: DataType,
        uid: Int,
        hero: Hero,
        oldFinalDame: Int,
        price: Float,
        rewardType: BLOCK_REWARD_TYPE
    )
    fun resetShieldHeroWithRock(
        uid: Int,
        network: String,
        hero: Hero,
        oldFinalDame: Int,
        price: Float,
        rewardType: BLOCK_REWARD_TYPE
    )

    fun getUserEmailInfo(uid: Int): ISFSObject
    fun registerEmail(uid: Int, email: String, verifyCode: String)
    fun verifyEmail(uid: Int, verifyCode: String): Boolean
    fun updateTrialUser(userName: String, uid: Int)
    fun syncDeposit(uid: Int, dataType: DataType, userDeposited: UserDeposited)

    fun updateMiningMode(uid: Int, tokenType: TokenType)

    fun updateLogoutInfo(uid: Int, deviceType: DeviceType)

    fun loadUserConfig(uid: Int, gachaChestSlotManager: IGachaChestSlotManager): UserConfig

    fun updateUserGachaChestSlot(
        uid: Int,
        dataType: DataType,
        price: Int,
        slot: Int,
        newSlotJson: String
    )

    fun increasePVPMatchCount(userId: Int, isWin: Boolean, gachaChestSlotManager: IGachaChestSlotManager)
    fun loadUserMaterial(uid: Int, configProductManager: IConfigItemManager): Map<Int, UserMaterial>
    fun mergerUserCrystal(
        uid: Int,
        sourceItemId: Int,
        targetItemId: Int,
        quantity: Int,
        mergerRate: Int,
        goldFee: Int,
        gemFee: Int
    )

    fun upgradeHeroTr(
        uid: Int,
        bomberId: Int,
        hp: Int,
        dmg: Int,
        speed: Int,
        range: Int,
        bomb: Int,
        upgradeConfig: UpgradeHeroTr
    )

    fun saveUserMiscConfig(userId: Int, config: MiscConfigs)
    fun getUserSubscription(uid: Int): List<UserSubscription>
    fun saveUserSubscription(
        uid: Int,
        product: SubscriptionProduct,
        startTime: Long,
        endTime: Long,
        packageToken: String,
        packageState: SubscriptionState,
    )

    fun updateTournamentResult(
        result: IPvpResultInfo,
    )

    fun countPvpPlayedMatch(uid: Int): Int
    fun deleteUserAccount(uid: Int)

    fun activeHeroTr(uid: Int, heroId: Int)

    fun saveUserConfigNoAds(userId: Int, isNoAds: Boolean)
    fun updateUserConfig(uid: Int, userConfig: UserConfig)
    fun loadUserOldItem(uid: Int): Set<ItemId>
    fun saveUserOldItem(uid: Int, itemIds: Set<ItemId>)
    fun loadUserCostumerPreset(uid: Int): List<IUserCostumePreset>
    fun saveUserCostumerPreset(uid: Int, item: IUserCostumePreset)
    fun queryAllUserAvatarActive(): MutableMap<Int, Int>
    fun queryUserAvatarActive(userId: Int): Int
    fun saveTelegramUser(idTelegram: String, deviceType: DeviceType): IUserInfo
    
    fun createTonTransaction(uid: Int): Int
    fun updateTonTransaction(id: Int, amount: Double, txHash: String, token: String): String
    fun updateBcoinTonTransaction(id: Int, amount: Double, txHash: String, token: String): String

    fun fusionHeroServer(uid: Int, heroIds: List<Int>, priceFusion: Double, rewardType: BLOCK_REWARD_TYPE, network: DataType): String
    fun getHeroPvp(userId: Int, heroId: Long): ISFSObject
    
    fun createSolTransaction(uid: Int): Int
    fun updateSolTransaction(id: Int, amount: Double, txHash: String, token: String): String
    fun updateBcoinSolTransaction(id: Int, amount: Double, txHash: String, token: String): String
    
    fun createRonTransaction(uid: Int): Int
    fun updateRonTransaction(id: Int, amount: Double, txHash: String, token: String, sender: String): String
    fun getOnBoardingConfig(): Map<Int, Float>
    fun getUserOnBoardingProgress(uid: Int): ISFSObject
    fun updateUserOnBoardingProgress(userProgress: UserProgress)
    
    fun createBasTransaction(uid: Int): Int
    fun updateBasTransaction(id: Int, amount: Double, txHash: String, token: String, sender: String): String
    
    fun getDailyTaskConfig(): List<DailyTask>
    fun getUserDailyTask(uid: Int): ISFSObject
    fun updateUserDailyTask(uid: Int, todayUserTask: TodayTask)
    fun logTodayTask(taskId: List<Int>)
    
    fun createVicTransaction(uid: Int): Int
    fun updateVicTransaction(id: Int, amount: Double, txHash: String, token: String, sender: String): String
}