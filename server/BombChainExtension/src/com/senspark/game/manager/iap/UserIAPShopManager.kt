package com.senspark.game.manager.iap

import com.senspark.common.utils.toSFSArray
import com.senspark.game.constant.ItemType
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.manager.hero.IConfigHeroTraditionalManager
import com.senspark.game.data.manager.iap.IIAPShopManager
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.manager.nativeRate.INativeRateManager
import com.senspark.game.data.manager.nativeRate.nativePriceEntry
import com.senspark.game.data.manager.nativeRate.pricesArray
import com.senspark.game.data.model.config.IAPShopConfig
import com.senspark.game.data.model.config.IAPShopConfigItem
import com.senspark.game.data.model.user.AddUserItemWrapper
import com.senspark.game.data.model.user.UserIAPPack
import com.senspark.game.db.IIapDataAccess
import com.senspark.game.db.IRewardDataAccess
import com.senspark.game.db.helper.QueryHelper
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.customEnum.ChangeRewardReason
import com.senspark.game.declare.customEnum.IAPShopType
import com.senspark.game.declare.customEnum.IapStore
import com.senspark.game.declare.customTypeAlias.IsToDayWasBought
import com.senspark.game.declare.customTypeAlias.ProductId
import com.senspark.game.declare.customTypeAlias.Quantity
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.config.IUserConfigManager
import com.senspark.game.manager.subscription.IUserSubscriptionManager
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject

class UserIAPShopManager(
    private val _mediator: UserControllerMediator,
    private val userSubscriptionManager: IUserSubscriptionManager,
    private val userConfigManager: IUserConfigManager,
    private val saveGameAndLoadReward: () -> Unit,
) : IUserIAPShopManager {

    private val rewardDataAccess = _mediator.services.get<IRewardDataAccess>()
    private val iapDataAccess = _mediator.services.get<IIapDataAccess>()

    private val iapShopManager = _mediator.svServices.get<IIAPShopManager>()
    private val configHeroTraditionalManager = _mediator.svServices.get<IConfigHeroTraditionalManager>()
    private val configItemManager = _mediator.svServices.get<IConfigItemManager>()
    private val nativeRateManager = _mediator.services.get<INativeRateManager>()
    
    private lateinit var purchasedPackages: Map<ProductId, Quantity>
    private lateinit var specialOffersWasBought: Map<ProductId, IsToDayWasBought>
    private lateinit var iapPacks: List<UserIAPPack>

    init {
        loadData()
    }

    private fun loadData() {
        purchasedPackages = iapDataAccess.getUserGemPacksPurchased(_mediator.userId)
        specialOffersWasBought = iapDataAccess.loadUserSpecialOfferWasBought(_mediator.userId)
        iapPacks = iapDataAccess.loadUserIapPack(_mediator.userId)
    }

    override fun getGemShop(): ISFSArray {
        return iapShopManager.getShopConfigs(IAPShopType.GEM).map {
            it.canBuySpecialOffer = !(specialOffersWasBought[it.productId] ?: false)
            it
        }.toSFSArray { it.toSFSObject(purchasedPackages) }
    }

    override fun getGemShopV3(): ISFSArray {
        return iapShopManager.getShopConfigs(IAPShopType.GEM).toSFSArray { config ->
            config.toSFSObject(purchasedPackages).apply {
                // canBuySpecialOffer is a mutable field on the shared config object; V2's per-user write
                // to it is a race V3 has no reason to join, so it's left unset rather than written.
                removeElement("can_buy_special_offer")

                // Empty prices[] means store-only: no native coin on this network, or no bcoin_price configured.
                val prices = pricesArray()
                val bcoinPrice = config.bcoinPrice
                val entry = bcoinPrice?.let { nativeRateManager.nativePriceEntry(_mediator.dataType, it) }
                if (entry == null) {
                    // Empty prices[] hides the pack on the client — log which input is missing.
                    val reason = if (bcoinPrice == null) {
                        "config_iap_shop.bcoin_price is null"
                    } else {
                        "no native coin on ${_mediator.dataType} (bcoin_price=$bcoinPrice)"
                    }
                    _mediator.logger.warn("[IAPShop] ${config.productId} has no native price: $reason")
                } else {
                    prices.addSFSObject(entry)
                }
                putSFSArray("prices", prices)
            }
        }
    }

    override fun saveUserIapPack(buyStep: Int) {
        var limitTime = 0L
        val iapPacks = iapShopManager.getShopConfigs(IAPShopType.PACK)
            .filter { (buyStep == 0 || it.buyStep == buyStep) && it.purchaseTimeLimit != null }
        val time = iapPacks.groupBy { it.buyStep }
            .toSortedMap()
            .mapValues {
                limitTime += it.value.firstOrNull()?.purchaseTimeLimit ?: 0
                limitTime
            }
        iapDataAccess.saveUserIapPack(
            _mediator.userId,
            iapPacks.map { Pair(it.productId, time[it.buyStep] ?: 0) }.toList()
        )
        this.iapPacks = iapDataAccess.loadUserIapPack(_mediator.userId)
    }

    override fun getPackShop(): List<ISFSObject> {
        val availablePacks = iapShopManager.getShopConfigs(IAPShopType.PACK)
            .filter {
                it.canBuyMore(purchasedPackages[it.productId] ?: 0) && !packWasExpired(it.productId)
            }
            .groupBy { it.isStaterPack }

        val response = if (availablePacks[true]?.isNotEmpty() == true) {
            availablePacks[true] ?: mutableListOf()
        } else {
            availablePacks[false] ?: mutableListOf()
        }.map {
            it.toSFSObject().apply {
                val saleEndDate = iapPacks.firstOrNull { it2 -> it2.productId == it.productId }?.saleEndDate
                if (saleEndDate != null) {
                    putLong("sale_end_date", saleEndDate)
                }
            }
        }
        return response
    }


    private fun packWasExpired(productId: ProductId): Boolean {
        val pack = iapPacks.firstOrNull { it.productId == productId }
        return pack?.packWasExpired() ?: false
    }


    override fun buy(
        type: IAPShopType,
        packageName: String,
        productId: String,
        billToken: String,
        transactionId: String,
        storeId: Int,
        isSpecialOffer: Boolean
    ) {
        _mediator.logger.log("nhanc19 UserIAPShopManager (1): $type, packageName: $packageName, productId: $productId, billToken: $billToken, transactionId: $transactionId, storeId: $storeId")
        val store = IapStore.fromValue(storeId)
        //check bill token was used
        if (rewardDataAccess.checkBillTokenExist(if (store == IapStore.GOOGLE_PLAY) billToken else transactionId))
            throw CustomException(
                "Invalid request, bill already used: $billToken",
                ErrorCode.IAP_SHOP_BILL_ALREADY_USED
            )

        val itemConfig = iapShopManager.getShopConfigs(type, productId)
        //check limit purchase of pack
        itemConfig.limitPerUser?.let {
            require(it > (purchasedPackages[productId] ?: 0)) {
                "Package can only be purchased $it times"
            }
        }
        val isTodayWasBoughtSpecialOffer = specialOffersWasBought[itemConfig.productId] ?: false
        if (isSpecialOffer && isTodayWasBoughtSpecialOffer) {
            throw CustomException("Special offers today have gone")
        }
        _mediator.logger.log("nhanc19 UserIAPShopManager (2): billToken: $billToken, transactionId: $transactionId")
        val result = iapShopManager.verifyBillInfo(packageName, productId, billToken, transactionId, store.storeName)
        _mediator.logger.log("nhanc19 UserIAPShopManager (3): billToken: $billToken, transactionId: $transactionId, buy result: $result")

        grantPurchase(itemConfig) { totalGemsReceive ->
            QueryHelper.queryInsertUserBuyGemTransaction(
                _mediator.userId,
                if (store == IapStore.GOOGLE_PLAY) billToken else transactionId,
                totalGemsReceive,
                productId,
                isSpecialOffer,
                result.isTest,
                result.region
            )
        }
    }

    // No isSpecialOffer parameter — a native purchase always records is_special_offer = FALSE
    // (see queryChargeNativeAndInsertUserBuyGemTransaction).
    override fun buyByNativeToken(productId: String) {
        val rewardType = _mediator.dataType.convertToNativeDepositType()
            ?: throw CustomException("Native token is not available on this network", ErrorCode.INVALID_PARAMETER)

        val itemConfig = iapShopManager.getShopConfigs(IAPShopType.GEM, productId)
        val bcoinPrice = itemConfig.bcoinPrice
            ?: throw CustomException("This package cannot be bought with native token", ErrorCode.INVALID_PARAMETER)

        itemConfig.limitPerUser?.let {
            require(it > (purchasedPackages[productId] ?: 0)) {
                "Package can only be purchased $it times"
            }
        }

        // Only GEM_LOCKED may be granted here: plain GEM is swappable to a withdrawable token, and a native
        // pack sells 25% under store price, so granting GEM would open deposit -> cheap gems -> swap -> withdraw.
        val grantsOnlyLockedGems = (itemConfig.items + itemConfig.itemsBonus).all {
            runCatching { BLOCK_REWARD_TYPE.fromItemId(it.itemId) }.getOrNull() == BLOCK_REWARD_TYPE.GEM_LOCKED
        }
        if (!grantsOnlyLockedGems) {
            throw CustomException("This package cannot be bought with native token", ErrorCode.INVALID_PARAMETER)
        }

        // user_buy_gem_transaction is keyed by the store's bill token, which an on-chain purchase has
        // none of. The prefix also separates on-chain from store revenue without a new column.
        val billToken = "native:${_mediator.dataType.name}:${_mediator.userId}:${System.currentTimeMillis()}"
        grantPurchase(itemConfig) { totalGemsReceive ->
            QueryHelper.queryChargeNativeAndInsertUserBuyGemTransaction(
                uid = _mediator.userId,
                network = _mediator.dataType,
                rewardType = rewardType,
                bcoinPrice = bcoinPrice,
                billToken = billToken,
                totalGemsReceive = totalGemsReceive,
                productId = productId,
                reason = ChangeRewardReason.BUY_GEM_BY_NATIVE_TOKEN,
            )
        }
    }

    /**
     * Everything after the payment step. [transactionQuery] runs inside the same transaction as the
     * credit, so a native charge that comes up short rolls the whole grant back.
     */
    private fun grantPurchase(
        itemConfig: IAPShopConfig,
        transactionQuery: (totalGemsReceive: Int) -> Pair<String, Array<Any?>>,
    ) {
        val bonusViaSubscription = userSubscriptionManager.gemPackageBonus
        val bonusItems = itemConfig.getItemBonus(purchasedPackages).ifEmpty {
            itemConfig.items.map {
                IAPShopConfigItem(it.itemId, (it.quantity * bonusViaSubscription).toInt(), it.expirationAfter)
            }
        }
        val totalItemsReceive = itemConfig.items.map {
            AddUserItemWrapper(
                configItemManager.getItem(it.itemId),
                it.quantity + bonusItems.filter { it2 -> it2.itemId == it.itemId }.sumOf { it2 -> it2.quantity },
                true,
                it.expirationAfter,
                _mediator.userId,
                configHeroTraditionalManager
            )
        }
        val totalGemsReceive = totalItemsReceive.filter {
            if (it.item.type == ItemType.REWARD) {
                val rewardType = BLOCK_REWARD_TYPE.fromItemId(it.item.id)
                rewardType == BLOCK_REWARD_TYPE.GEM || rewardType == BLOCK_REWARD_TYPE.GEM_LOCKED
            } else false
        }.sumOf { it.quantity }

        rewardDataAccess.addTRRewardForUser(
            uid = _mediator.userId,
            dataType = _mediator.dataType,
            rewardReceives = totalItemsReceive,
            reloadRewardAfterAdd = {
                iapDataAccess.clearUserBuyGemTransactionCache(_mediator.userId)
                loadData()
                saveGameAndLoadReward()
            },
            "Buy",
            rewardSpent = emptyMap(),
            additionUpdateQueries = listOf(transactionQuery(totalGemsReceive))
        )
        if (itemConfig.isRemoveAds) {
            userConfigManager.setNoAds()
        }
        if (itemConfig.type == IAPShopType.PACK) {
            saveUserIapPack(itemConfig.buyStep + 1)
        }
    }
}