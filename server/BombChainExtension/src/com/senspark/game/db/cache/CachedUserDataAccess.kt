package com.senspark.game.db.cache

import com.google.gson.JsonArray
import com.senspark.common.cache.ICacheService
import com.senspark.common.constant.ItemId
import com.senspark.game.api.IPvpResultInfo
import com.senspark.game.constant.CachedKeys
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
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.db.model.UserConfig
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.customEnum.SubscriptionProduct
import com.senspark.game.declare.customEnum.SubscriptionState
import com.senspark.game.manager.config.MiscConfigs
import com.senspark.game.manager.dailyTask.DailyTask
import com.senspark.game.manager.dailyTask.TodayTask
import com.senspark.game.manager.dailyTask.UserTask
import com.senspark.game.manager.onBoarding.UserProgress
import com.senspark.game.utils.deserializeList
import com.senspark.game.utils.serialize
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import java.time.LocalDate
import kotlin.time.Duration.Companion.hours


class CachedUserDataAccess(
    private val _bridge: IUserDataAccess,
    private val _cache: ICacheService
) : IUserDataAccess {

    override fun initialize() {
    }

    override fun getUserInfo(info: IUserLoginInfo, deviceType: EnumConstants.DeviceType): IUserInfo {
        return _bridge.getUserInfo(info, deviceType)
    }

    override fun getDisplayNameUser(uid: Int): String {
        return _bridge.getDisplayNameUser(uid)
    }


    override fun saveUserLoginInfo(info: IUserLoginInfo, deviceType: EnumConstants.DeviceType): IUserInfo {
        return _bridge.saveUserLoginInfo(info, deviceType)
    }

    override fun changeMiningToken(uid: Int, tokenType: EnumConstants.TokenType): Boolean {
        return _bridge.changeMiningToken(uid, tokenType)
    }

    override fun loadUserPvpBoosters(uid: Int): Map<Int, UserBooster> {
        return _bridge.loadUserPvpBoosters(uid)
    }

    override fun changeDefaultMiningToken(uid: Int, tokenType: EnumConstants.TokenType): Boolean {
        return _bridge.changeDefaultMiningToken(uid, tokenType)
    }

    override fun getConfigPvpRankingReward(): List<PvpRankingReward> {
        return _bridge.getConfigPvpRankingReward()
    }

    override fun loadUserStakeVip(username: String): List<UserStakeVipReward> {
        return _bridge.loadUserStakeVip(username)
    }

    override fun claimStakeVipReward(
        uid: Int,
        dataType: EnumConstants.DataType,
        userStakeVipReward: UserStakeVipReward
    ) {
        return _bridge.claimStakeVipReward(uid, dataType, userStakeVipReward)
    }

    override fun subUserPvpBoosters(uid: Int, boosterIds: List<Int>) {
        return _bridge.subUserPvpBoosters(uid, boosterIds)
    }

    override fun summaryPvpRankingReward() {
        return _bridge.summaryPvpRankingReward()
    }

    override fun isSummaryStoryHunterSeason(): Boolean {
        return _bridge.isSummaryStoryHunterSeason()
    }

    override fun buyItemMarketplace(
        item: Item,
        quantity: Int,
        unitPrice: Float,
        buyerId: Int,
        rewardType: EnumConstants.BLOCK_REWARD_TYPE,
        expirationAfter: Int
    ) {
        return _bridge.buyItemMarketplace(item, quantity, unitPrice, buyerId, rewardType, expirationAfter)
    }

    override fun sellItemMarketplace(
        listId: List<Int>,
        type: Int,
        itemId: Int,
        quantity: Int,
        price: Float,
        uid: Int,
        rewardType: Int,
        expirationAfter: Int
    ) {
        return _bridge.sellItemMarketplace(listId, type, itemId, quantity, price, uid, rewardType, expirationAfter)
    }

    override fun cancelItemMarketplace(uid: Int, itemId: Int, quantity: Int, unitPrice: Float, expirationAfter: Int) {
        return _bridge.cancelItemMarketplace(uid, itemId, quantity, unitPrice, expirationAfter)
    }

    override fun editItemMarketplace(
        uid: Int,
        itemId: Int,
        oldQuantity: Int,
        oldUnitPrice: Float,
        newQuantity: Int,
        newUnitPrice: Float,
        expirationAfter: Int
    ) {
        return _bridge.editItemMarketplace(
            uid,
            itemId,
            oldQuantity,
            oldUnitPrice,
            newQuantity,
            newUnitPrice,
            expirationAfter
        )
    }

    override fun getPvpRankingReward(pvpMatchReward: Int, season: Int): Map<Int, UserPvpRankingReward> {
        return _bridge.getPvpRankingReward(pvpMatchReward, season)
    }

    override fun getStoryHunterBuyTicket(season: Int): Int {
        return _bridge.getStoryHunterBuyTicket(season)
    }

    override fun isLoadDataManager(schedule: String, season: Int): Boolean {
        return _bridge.isLoadDataManager(schedule, season)
    }

    override fun logRepairShield(username: String, dataType: EnumConstants.DataType, repairedBomberman: Map<Int, Int>) {
        return _bridge.logRepairShield(username, dataType, repairedBomberman)
    }

    override fun updateAccountName(
        dataType: EnumConstants.DataType,
        username: String,
        newName: String,
        fee: Float,
        deposit: Float,
        feeRewardType: EnumConstants.BLOCK_REWARD_TYPE
    ) {
        return _bridge.updateAccountName(dataType, username, newName, fee, deposit, feeRewardType)
    }

    override fun getNextRoundStoryHunter(userName: String): Int {
        return _bridge.getNextRoundStoryHunter(userName)
    }

    override fun logPvpBooster(type: String, itemId: Int, boosterName: String, userId: Int, boosterPrice: Int) {
        return _bridge.logPvpBooster(type, itemId, boosterName, userId, boosterPrice)
    }

    override fun hasClaimReward(userId: Int, season: Int): Boolean {
        return _bridge.hasClaimReward(userId, season)
    }

    override fun insertLogRewardDeposit(userName: String, rewardType: String, rewardAmount: Float) {
        return _bridge.insertLogRewardDeposit(userName, rewardType, rewardAmount)
    }

    override fun getUserInventory(uid: Int, filter: Filter, status: Int): MutableMap<ItemId, MutableList<UserItem>> {
        return _bridge.getUserInventory(uid, filter, status)
    }

    override fun getUserBooster(uid: Int, filter: Filter): Map<Int, List<UserItem>> {
        return _bridge.getUserBooster(uid, filter)
    }

    override fun getMarketplaceItem(id: Int, configProductManager: IConfigItemManager): MarketplaceItem {
        return _bridge.getMarketplaceItem(id, configProductManager)
    }

    override fun getTotalCountItemInMarket(excludeOwnerId: Int, filter: Filter): Int {
        return _bridge.getTotalCountItemInMarket(excludeOwnerId, filter)
    }

    override fun getActivity(uid: Int): Map<Int, Activity> {
        return _bridge.getActivity(uid)
    }

    override fun activateCode(uid: Int, code: String): Boolean {
        return _bridge.activateCode(uid, code)
    }

    override fun buyAutoMinePackage(
        uid: Int,
        autoMinePackage: AutoMinePackage,
        dataType: EnumConstants.DataType,
        firstRewardType: String,
        secondRewardType: String
    ) {
        return _bridge.buyAutoMinePackage(uid, autoMinePackage, dataType, firstRewardType, secondRewardType)
    }

    override fun buyRockPackage(
        uid: Int,
        packageName: String,
        network: String,
        dataType: String,
        secondDataType: String
    ) {
        return _bridge.buyRockPackage(uid, packageName, network, dataType, secondDataType)
    }

    override fun loadUserAutoMinePackage(uid: Int, dataType: EnumConstants.DataType): IUserAutoMine {
        return _bridge.loadUserAutoMinePackage(uid, dataType)
    }

    /**
     * Mỗi ngày thay 1 lần
     */
    override fun loadAutoMinePackagePrice(uid: Int, listArrayPackage: JsonArray): ISFSArray {
        val date = LocalDate.now().dayOfMonth
        val field = "${uid}_${date}"
        try {
            val cached = _cache.getFromHash(CachedKeys.AUTO_MINE_PRICE, field)
            if (cached.isNullOrEmpty()) {
                throw Exception("Empty cache")
            }
            return SFSArray.newFromJsonData(cached)
        } catch (e: Exception) {
            val result = _bridge.loadAutoMinePackagePrice(uid, listArrayPackage)
            _cache.setToHash(CachedKeys.AUTO_MINE_PRICE, field, result.toJson(), 24.hours)
            return result
        }
    }

    override fun resetShieldHero(
        dataType: EnumConstants.DataType,
        uid: Int,
        hero: Hero,
        oldFinalDame: Int,
        price: Float,
        rewardType: EnumConstants.BLOCK_REWARD_TYPE
    ) {
        return _bridge.resetShieldHero(dataType, uid, hero, oldFinalDame, price, rewardType)
    }

    override fun resetShieldHeroWithRock(
        uid: Int,
        network: String,
        hero: Hero,
        oldFinalDame: Int,
        price: Float,
        rewardType: EnumConstants.BLOCK_REWARD_TYPE
    ) {
        return _bridge.resetShieldHeroWithRock(uid, network, hero, oldFinalDame, price, rewardType)
    }

    override fun getUserEmailInfo(uid: Int): ISFSObject {
        return _bridge.getUserEmailInfo(uid)
    }

    override fun registerEmail(uid: Int, email: String, verifyCode: String) {
        return _bridge.registerEmail(uid, email, verifyCode)
    }

    override fun verifyEmail(uid: Int, verifyCode: String): Boolean {
        return _bridge.verifyEmail(uid, verifyCode)
    }

    override fun updateTrialUser(userName: String, uid: Int) {
        return _bridge.updateTrialUser(userName, uid)
    }

    override fun syncDeposit(uid: Int, dataType: EnumConstants.DataType, userDeposited: UserDeposited) {
        return _bridge.syncDeposit(uid, dataType, userDeposited)
    }

    override fun updateMiningMode(uid: Int, tokenType: EnumConstants.TokenType) {
        return _bridge.updateMiningMode(uid, tokenType)
    }

    override fun updateLogoutInfo(uid: Int, deviceType: EnumConstants.DeviceType) {
        return _bridge.updateLogoutInfo(uid, deviceType)
    }

    override fun loadUserConfig(uid: Int, gachaChestSlotManager: IGachaChestSlotManager): UserConfig {
        return _bridge.loadUserConfig(uid, gachaChestSlotManager)
    }

    override fun updateUserGachaChestSlot(
        uid: Int,
        dataType: EnumConstants.DataType,
        price: Int,
        slot: Int,
        newSlotJson: String
    ) {
        return _bridge.updateUserGachaChestSlot(uid, dataType, price, slot, newSlotJson)
    }

    override fun increasePVPMatchCount(userId: Int, isWin: Boolean, gachaChestSlotManager: IGachaChestSlotManager) {
        return _bridge.increasePVPMatchCount(userId, isWin, gachaChestSlotManager)
    }

    override fun loadUserMaterial(uid: Int, configProductManager: IConfigItemManager): Map<Int, UserMaterial> {
        return _bridge.loadUserMaterial(uid, configProductManager)
    }

    override fun mergerUserCrystal(
        uid: Int,
        sourceItemId: Int,
        targetItemId: Int,
        quantity: Int,
        mergerRate: Int,
        goldFee: Int,
        gemFee: Int
    ) {
        return _bridge.mergerUserCrystal(uid, sourceItemId, targetItemId, quantity, mergerRate, goldFee, gemFee)
    }

    override fun upgradeHeroTr(
        uid: Int,
        bomberId: Int,
        hp: Int,
        dmg: Int,
        speed: Int,
        range: Int,
        bomb: Int,
        upgradeConfig: UpgradeHeroTr
    ) {
        return _bridge.upgradeHeroTr(uid, bomberId, hp, dmg, speed, range, bomb, upgradeConfig)
    }

    override fun saveUserMiscConfig(userId: Int, config: MiscConfigs) {
        return _bridge.saveUserMiscConfig(userId, config)
    }

    override fun getUserSubscription(uid: Int): List<UserSubscription> {
        return _bridge.getUserSubscription(uid)
    }

    override fun saveUserSubscription(
        uid: Int,
        product: SubscriptionProduct,
        startTime: Long,
        endTime: Long,
        packageToken: String,
        packageState: SubscriptionState
    ) {
        return _bridge.saveUserSubscription(uid, product, startTime, endTime, packageToken, packageState)
    }

    override fun updateTournamentResult(result: IPvpResultInfo) {
        return _bridge.updateTournamentResult(result)
    }

    override fun countPvpPlayedMatch(uid: Int): Int {
        return _bridge.countPvpPlayedMatch(uid)
    }

    override fun deleteUserAccount(uid: Int) {
        return _bridge.deleteUserAccount(uid)
    }

    override fun activeHeroTr(uid: Int, heroId: Int) {
        return _bridge.activeHeroTr(uid, heroId)
    }

    override fun saveUserConfigNoAds(userId: Int, isNoAds: Boolean) {
        return _bridge.saveUserConfigNoAds(userId, isNoAds)
    }

    override fun updateUserConfig(uid: Int, userConfig: UserConfig) {
        return _bridge.updateUserConfig(uid, userConfig)
    }

    /**
     * Read
     */
    override fun loadUserOldItem(uid: Int): Set<ItemId> {
        val field = uid.toString()
        return try {
            val value = _cache.getFromHash(CachedKeys.USER_OLD_ITEM, field)
            if (value.isNullOrEmpty()) throw Exception("Empty cache")
            deserializeList<ItemId>(value).toSet()
        } catch (e: Exception) {
            val result = _bridge.loadUserOldItem(uid)
            val value = result.serialize()
            _cache.setToHash(CachedKeys.USER_OLD_ITEM, field, value)
            result
        }
    }

    /**
     * Write (Invalidate cache)
     */
    override fun saveUserOldItem(uid: Int, itemIds: Set<ItemId>) {
        val field = uid.toString()
        _bridge.saveUserOldItem(uid, itemIds)
        val value = itemIds.serialize()
        _cache.setToHash(CachedKeys.USER_OLD_ITEM, field, value)
    }

    /**
     * Read
     */
    override fun loadUserCostumerPreset(uid: Int): List<IUserCostumePreset> {
        val field = uid.toString()
        try {
            return deserializeList<IUserCostumePreset>(_cache.getFromHash(CachedKeys.COSTUME_PRESET, field)!!)
        } catch (e: Exception) {
            val result = _bridge.loadUserCostumerPreset(uid)
            _cache.setToHash(CachedKeys.COSTUME_PRESET, field, result.serialize())
            return result
        }
    }

    /**
     * Write (Invalidate cache)
     */
    override fun saveUserCostumerPreset(uid: Int, item: IUserCostumePreset) {
        val field = uid.toString()
        _bridge.saveUserCostumerPreset(uid, item)
        _cache.deleteFromHash(CachedKeys.COSTUME_PRESET, field)
    }

    override fun queryAllUserAvatarActive(): MutableMap<Int, Int> {
        return _bridge.queryAllUserAvatarActive()
    }

    override fun queryUserAvatarActive(userId: Int): Int {
        return _bridge.queryUserAvatarActive(userId)
    }

    override fun saveTelegramUser(idTelegram: String, deviceType: EnumConstants.DeviceType): IUserInfo {
        return _bridge.saveTelegramUser(idTelegram, deviceType)
    }

    override fun createTonTransaction(uid: Int): Int {
        return _bridge.createTonTransaction(uid)
    }

    override fun updateTonTransaction(id: Int, amount: Double, txHash: String, token: String): String {
        return _bridge.updateTonTransaction(id, amount, txHash, token)
    }

    override fun updateBcoinTonTransaction(id: Int, amount: Double, txHash: String, token: String): String {
        return _bridge.updateBcoinTonTransaction(id, amount, txHash, token)
    }

    override fun fusionHeroServer(
        uid: Int,
        heroIds: List<Int>,
        priceFusion: Double,
        rewardType: EnumConstants.BLOCK_REWARD_TYPE,
        network: EnumConstants.DataType
    ): String {
        return _bridge.fusionHeroServer(uid, heroIds, priceFusion, rewardType, network)
    }

    override fun getHeroPvp(userId: Int, heroId: Long): ISFSObject {
        return _bridge.getHeroPvp(userId, heroId)
    }

    override fun createSolTransaction(uid: Int): Int {
        return _bridge.createSolTransaction(uid)
    }

    override fun updateSolTransaction(id: Int, amount: Double, txHash: String, token: String): String {
        return _bridge.updateSolTransaction(id, amount, txHash, token)
    }
    
    override fun updateBcoinSolTransaction(id: Int, amount: Double, txHash: String, token: String): String =
        _bridge.updateBcoinSolTransaction(id, amount, txHash, token)

    override fun createRonTransaction(uid: Int): Int = _bridge.createRonTransaction(uid)

    override fun updateRonTransaction(id: Int, amount: Double, txHash: String, token: String, sender: String): String =
        _bridge.updateRonTransaction(id, amount, txHash, token, sender)

    override fun getOnBoardingConfig(): Map<Int, Float> {
        return _bridge.getOnBoardingConfig()
    }

    override fun createBasTransaction(uid: Int): Int = _bridge.createBasTransaction(uid)

    override fun updateBasTransaction(id: Int, amount: Double, txHash: String, token: String, sender: String): String =
        _bridge.updateBasTransaction(id, amount, txHash, token, sender)

    override fun getUserOnBoardingProgress(uid: Int): ISFSObject {
        return _bridge.getUserOnBoardingProgress(uid)
    }

    override fun updateUserOnBoardingProgress(userProgress: UserProgress) {
        return _bridge.updateUserOnBoardingProgress(userProgress)
    }

    override fun getDailyTaskConfig(): List<DailyTask> {
        return _bridge.getDailyTaskConfig()
    }

    override fun getUserDailyTask(uid: Int): ISFSObject {
        return _bridge.getUserDailyTask(uid)
    }

    override fun updateUserDailyTask(uid: Int, todayUserTask: TodayTask) {
        return _bridge.updateUserDailyTask(uid, todayUserTask)
    }

    override fun logTodayTask(taskId: List<Int>) {
        return _bridge.logTodayTask(taskId)
    }

    override fun createVicTransaction(uid: Int): Int {
        return _bridge.createVicTransaction(uid)
    
    }

    override fun updateVicTransaction(
        id: Int,
        amount: Double,
        txHash: String,
        token: String,
        sender: String
    ): String {
        return _bridge.updateVicTransaction(id, amount, txHash, token, sender)
    }
}