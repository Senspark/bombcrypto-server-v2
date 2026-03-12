package com.senspark.game.manager.gachaChest

import com.senspark.common.utils.toSFSArray
import com.senspark.game.api.IVerifyAdApiManager
import com.senspark.game.controller.IUserController
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.manager.hero.IConfigHeroTraditionalManager
import com.senspark.game.data.model.config.IGachaChest
import com.senspark.game.data.model.config.IGachaChestItem
import com.senspark.game.data.model.user.AddUserItemWrapper
import com.senspark.game.data.model.user.UserGachaChest
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IRewardDataAccess
import com.senspark.game.db.gachaChest.IGachaChestDataAccess
import com.senspark.game.db.helper.QueryHelper
import com.senspark.game.db.model.UserGachaChestSlot
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.KickReason
import com.senspark.game.declare.customEnum.BanReason
import com.senspark.game.declare.customEnum.GachaChestType
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.blockReward.IUserBlockRewardManager
import com.senspark.game.manager.config.IUserConfigManager
import com.senspark.game.manager.hero.IUserHeroTRManager
import com.senspark.game.user.GachaChestManager.Companion.SKIP_TIME_PER_ADS_IN_MILLIS
import com.senspark.game.user.IGachaChestManager
import com.smartfoxserver.v2.entities.data.ISFSArray

class UserGachaChestController(
    private val _mediator: UserControllerMediator,
    private val userConfigManager: IUserConfigManager,
    private val userHeroTRManager: IUserHeroTRManager,
    private val userBlockRewardManager: IUserBlockRewardManager,
    private val _verifyAdApi: IVerifyAdApiManager,
) : IUserGachaChestController {

    private val configHeroTraditionalManager = _mediator.svServices.get<IConfigHeroTraditionalManager>()
    private val gachaChestManager = _mediator.svServices.get<IGachaChestManager>()
    private val dataAccessManager = _mediator.services.get<IDataAccessManager>()
    private val rewardDataAccess: IRewardDataAccess = dataAccessManager.rewardDataAccess
    private val gachaChestDataAccess: IGachaChestDataAccess = dataAccessManager.gachaChestDataAccess

    private lateinit var havingChest: Map<Int, UserGachaChest>

    private fun getHavingChest(): Map<Int, UserGachaChest> {
        if (!::havingChest.isInitialized) {
            loadChest()
        }
        return havingChest
    }

    private fun loadChest() {
        havingChest = gachaChestDataAccess.getUserGachaChests(_mediator.userId, gachaChestManager).associateBy { it.id }
    }

    private fun getChest(id: Int): UserGachaChest {
        return getHavingChest()[id] ?: throw CustomException("Chest with id $id not exists")
    }

    override fun buyAndOpenGachaChest(
        chestType: GachaChestType,
        quantity: Int,
        rewardType: BLOCK_REWARD_TYPE,
        controller: IUserController
    ): ISFSArray {
        if (quantity <= 0 || quantity > 15) {
            controller.ban(1, BanReason.BUY_GACHA_CHEST, null)
            controller.disconnect(KickReason.HACK_CHEAT)
            throw CustomException("Error quantity")
        }
        val chest = gachaChestManager.getChestShop(chestType)
        val price = chest.getPrice(rewardType, quantity)
        if (price <= 0) {
            throw CustomException("Error price")
        }
        userBlockRewardManager.checkEnoughReward(price.toFloat(), rewardType)
        val items = randomAndSaveItem(chest, quantity, rewardType, price)

        return items.toSFSArray { it.toSfsObject() }
    }

    /**
     * random item in chest by quantity, save ro db and reload data cached
     *
     * @param quantity times to generate
     * @param price reward spent default null (case free)
     * @param rewardType type of reward spent
     * @param deleteUserChestId id of user chest need delete
     * @return list of randomized items
     */
    private fun randomAndSaveItem(
        chest: IGachaChest,
        quantity: Int,
        rewardType: BLOCK_REWARD_TYPE? = null,
        price: Int? = null,
        deleteUserChestId: Int? = null,
    ): List<IGachaChestItem> {
        val items = (1..quantity)
            .map { chest.randomItems() }.flatten()
        val addRewardItems = items.groupBy { Triple(it.item, it.expirationAfter, it.isLock) }
            .mapValues { it.value.sumOf { it2 -> it2.quantity } }
            .map {
                AddUserItemWrapper(
                    it.key.first,
                    it.value,
                    it.key.third,
                    expirationAfter = it.key.second,
                    userId = _mediator.userId,
                    configHeroTraditional = configHeroTraditionalManager
                )
            }
        val rewardSpent = price.let {
            if (it != null) {
                require(rewardType != null) { "Reward spent type must not null" }
                if (it > 0) mapOf(rewardType to it.toFloat()) else emptyMap()
            } else {
                emptyMap()
            }
        }
        val additionUpdateQueries = mutableListOf<Pair<String, Array<Any?>>>()
        if (price != null) {
            additionUpdateQueries.add(
                QueryHelper.queryInsertLogBuyChestGacha(
                    _mediator.userId,
                    chest.type.name,
                    price
                )
            )
        }
        if (deleteUserChestId != null) {
            additionUpdateQueries.add(QueryHelper.queryDeleteUserGachaChest(deleteUserChestId))
        }
        rewardDataAccess.addTRRewardForUser(
            _mediator.userId,
            _mediator.dataType,
            addRewardItems,
            {
                userBlockRewardManager.loadUserBlockReward()
                userHeroTRManager.loadHero(true)
                loadChest()
            },
            "Gacha_chest",
            rewardSpent = rewardSpent,
            additionUpdateQueries = additionUpdateQueries
        )
        return items
    }

    override fun startOpeningGachaChest(chestId: Int): Long {
        val chest = getChest(chestId)
        if (isStillHaveWaitingChest()) {
            throw CustomException("User has already opened a chest before")
        }
        gachaChestDataAccess.startOpeningGachaChest(chest)
        return chest.remainingOpenTime
    }

    override fun openChest(id: Int): ISFSArray {
        val chest = getChest(id)
//        if (chest.openTime < 0) {
//            throw Exception("This chest still not start opening")
//        }
//        if (chest.remainingOpenTime > 0) {
//            throw Exception("This chest still not ready to open at current, remain: ${chest.remainingOpenTime}")
//        }
        val items = randomAndSaveItem(chest.chestConfig, 1, deleteUserChestId = chest.id)
        loadChest()
        return items.toSFSArray { it.toSfsObject() }
    }


    private fun isStillHaveWaitingChest(): Boolean {
        return getHavingChest().any {
            it.value.remainingOpenTime > 0
        }
    }

    override fun getAllChests(): List<UserGachaChestSlot> {
        val currentChest = getHavingChest().values.toMutableList()
        val chestSlots = userConfigManager.userGachaChestSlots
        chestSlots.forEach {
            if (it.isOwner && currentChest.isNotEmpty()) {
                it.chest = currentChest[0]
                currentChest.removeAt(0)
            } else {
                it.chest = null
            }
        }
        return chestSlots
    }

    override fun addChestFromBlockRewardType(blockRewardType: BLOCK_REWARD_TYPE): UserGachaChest? {
        val chestType = when (blockRewardType) {
            BLOCK_REWARD_TYPE.BRONZE_CHEST -> GachaChestType.BRONZE
            BLOCK_REWARD_TYPE.SILVER_CHEST -> GachaChestType.SILVER
            BLOCK_REWARD_TYPE.GOLD_CHEST -> GachaChestType.GOLD
            BLOCK_REWARD_TYPE.PLATINUM_CHEST -> GachaChestType.PLATINUM
            else -> throw CustomException("Chest with type ${blockRewardType.name} invalid")

        }
        val skipTimeBecauseFirstChest = !userConfigManager.isReceivedFirstChestSkipTime
        val result = gachaChestDataAccess.addGachaChestForUser(
            _mediator.userId,
            chestType,
            userConfigManager.numberChestSlot,
            gachaChestManager,
            true
        )
        if (skipTimeBecauseFirstChest) {
            userConfigManager.setReceivedFirstChestSkipTime()
        }
        loadChest()
        return result
    }

    override fun skipOpenTimeByGem(chestId: Int) {
        val chest = getChest(chestId)
        if (chest.openTime < 0) {
            throw Exception("This chest still not start opening")
        }
        if (chest.openTime == 0L) {
            throw CustomException("Chest is ready to open")
        }
        val getPrice = chest.chestConfig.skipOpenTimeGemRequire
        userBlockRewardManager.checkEnoughReward(getPrice.toFloat(), BLOCK_REWARD_TYPE.GEM)
        gachaChestDataAccess.skipOpenTimeByGem(_mediator.userId, chestId, getPrice)
        loadChest()
    }

    override suspend fun skipOpenTimeByAds(chestId: Int, token: String): Long {
        val isValid = _verifyAdApi.isValidAds(token)
        if (!isValid) {
            throw Exception("Invalid token: $token")
        }
        val chest = getChest(chestId)
        if (chest.openTime < 0) throw Exception("This chest still not start opening")
        if (chest.openTime == 0L) throw CustomException("Chest is ready to open")
        gachaChestDataAccess.skipOpenTimeByAds(_mediator.userId, chest, SKIP_TIME_PER_ADS_IN_MILLIS)
        loadChest()
        return chest.remainingOpenTime
    }
}