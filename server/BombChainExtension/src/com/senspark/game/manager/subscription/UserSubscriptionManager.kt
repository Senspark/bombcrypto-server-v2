package com.senspark.game.manager.subscription

import com.senspark.common.utils.toSFSArray
import com.senspark.game.api.subscription.ISubscriptionApi
import com.senspark.game.api.subscription.VerifySubscription
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.manager.subscription.ISubscriptionManager
import com.senspark.game.data.model.config.SubscriptionPackage
import com.senspark.game.data.model.user.UserSubscription
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IRewardDataAccess
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.db.helper.QueryHelper
import com.senspark.game.declare.customEnum.IapStore
import com.senspark.game.declare.customEnum.SubscriptionProduct
import com.senspark.game.manager.config.IUserConfigManager
import com.senspark.lib.utils.TimeUtils
import com.smartfoxserver.v2.entities.data.ISFSArray
import java.time.Instant

class UserSubscriptionManager(
    private val _mediator: UserControllerMediator,
    private val verifySubscriptionApi: ISubscriptionApi,
) : IUserSubscriptionManager {
    private val subscriptionManager = _mediator.svServices.get<ISubscriptionManager>()
    private val itemIConfigItemManager = _mediator.svServices.get<IConfigItemManager>()
    private val dataAccessManager = _mediator.services.get<IDataAccessManager>()
    private val userDataAccess: IUserDataAccess = dataAccessManager.userDataAccess
    private val rewardDataAccess: IRewardDataAccess = dataAccessManager.rewardDataAccess
    
    private val locker = Any()
    private var lastTimeSync: Instant? = null
    private lateinit var userSubscriptions: List<UserSubscription>
    private var validSubscription: UserSubscription? = null

    private lateinit var userConfigManager: IUserConfigManager
    
    override fun getValidSubscription(): UserSubscription? {
        if(!::userSubscriptions.isInitialized) {
            loadPackage()
        }
        return validSubscription
    }

    private fun loadPackage() {
        val now = Instant.now().epochSecond
        if (!::userSubscriptions.isInitialized) {
            userSubscriptions = userDataAccess.getUserSubscription(_mediator.userId)
        }
        validSubscription = userSubscriptions
            .filter { (now in it.startTime.epochSecond..it.endTime.epochSecond) }
            .maxByOrNull { it.product.level }
    }

    private fun loadCurrentPackage() {
        synchronized(locker) {
            loadPackage()
            userSubscriptions.maxByOrNull { it.lastModify }?.let {
                val response = verifySubscriptionApi.verifySubscription(it.product, it.token, it.redirect)
                updatePackage(it.product, response, it.token)
                loadPackage()
            }
            lastTimeSync = Instant.now()
        }
    }

    override fun initUserConfigManager(userConfigManager: IUserConfigManager) {
        if (!this::userConfigManager.isInitialized) {
            this.userConfigManager = userConfigManager
        }
    }

    override val subscriptionPackages: ISFSArray
        get() = subscriptionManager.configList.toSFSArray {
            val obj = it.toSfsObject()
            validSubscription.let { it2 ->
                if (it2 != null && it.id === it2.product) {
                    obj.putSFSObject("user_package", it2.toSfsObject())
                } else {
                    obj.putNull("user_package")
                }
            }
            obj
        }

    private fun updatePackage(
        subscriptionProduct: SubscriptionProduct,
        value: VerifySubscription,
        userToken: String
    ) {
        userDataAccess.saveUserSubscription(
            _mediator.userId,
            subscriptionProduct,
            value.startTime.epochSecond,
            value.endTime.epochSecond,
            userToken,
            value.state
        )
    }

    override val subscriptionPackage: SubscriptionPackage?
        get() {
            lastTimeSync.let {
                if (it == null || !TimeUtils.isSameDate(it)) {
                    loadCurrentPackage()
                }
            }
            validSubscription.let { sub ->
                return if (sub == null) null else subscriptionManager.getSubscription(sub.product)
            }

        }

    override val noAds: Boolean
        get() {
            return (subscriptionPackage?.noAds ?: false)
        }

    override val offlineRewardBonus: Int
        get() = subscriptionPackage?.offlineRewardBonus ?: 0

    override val pvpPenalty: Boolean
        get() = subscriptionPackage?.pvpPenalty ?: true

    override val bonusPvpPoint: Float
        get() = subscriptionPackage?.bonusPvpPointRate ?: 0f

    override val adventureBonusItems: Float
        get() = subscriptionPackage?.adventureBonusItemRate ?: 0f

    override val gemPackageBonus: Float
        get() = subscriptionPackage?.gemPackageBonus ?: 0f

    override fun subscribe(subscriptionProduct: SubscriptionProduct, userToken: String, store: IapStore) {
        val response = verifySubscriptionApi.verifySubscription(subscriptionProduct, userToken, store)
        updatePackage(subscriptionProduct, response, userToken)
        loadCurrentPackage()
    }

    override fun cancelSubscribe(subscriptionProduct: SubscriptionProduct) {
        validSubscription.let {
            if (it == null || it.product != subscriptionProduct) {
                throw Exception("Invalid subscription")
            } else {
                verifySubscriptionApi.cancelSubscription(subscriptionProduct, it.token, it.redirect)
                val response = verifySubscriptionApi.verifySubscription(subscriptionProduct, it.token, it.redirect)
                updatePackage(subscriptionProduct, response, it.token)
                loadCurrentPackage()
            }
        }
    }

    override fun takeSubscriptionRewards() {
        synchronized(locker) {
            var canTakeReward = true
            subscriptionPackage?.let { sp ->
                userConfigManager.lastTimeClaimSubscription?.let {
                    if (TimeUtils.isSameDate(it)) {
                        canTakeReward = false
                    }
                }
                if (canTakeReward) {
                    val rewards = sp.getRewards(itemIConfigItemManager)
                    rewardDataAccess.addTRRewardForUser(
                        _mediator.userId,
                        _mediator.dataType,
                        rewards,
                        {},
                        "Subscription",
                        additionUpdateQueries = listOf(QueryHelper.queryUpdateLastClaimSubscription(_mediator.userId))
                    )
                    userConfigManager.reloadConfig()
                }
            }
        }
    }
}