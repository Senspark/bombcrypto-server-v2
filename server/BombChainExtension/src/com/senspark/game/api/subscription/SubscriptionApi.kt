package com.senspark.game.api.subscription

import com.senspark.game.api.IRestApi
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.declare.customEnum.IapStore
import com.senspark.game.declare.customEnum.SubscriptionProduct
import com.senspark.game.declare.customEnum.SubscriptionState
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.IEnvManager
import com.senspark.game.utils.deserialize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

class SubscriptionApi(
    mediator: UserControllerMediator,
    private val api: IRestApi,
) : ISubscriptionApi {

    private val envManager: IEnvManager = mediator.services.get()
    
    override fun verifySubscription(
        subscriptionProduct: SubscriptionProduct,
        userToken: String,
        redirect: IapStore,
    ): VerifySubscription {
        val bodyJson = api.post(
            "${envManager.apMonetizationPath}/subscription/verify",
            envManager.apSignatureToken,
            buildBody(subscriptionProduct, userToken, redirect)
        )
        val response = deserialize<VerifySubscriptionResponse>(bodyJson)
        if (response.statusCode != 0) {
            throw CustomException("Verify failed, statusCode ${response.statusCode}")
        }
        return response.message
    }

    private fun buildBody(
        subscriptionProduct: SubscriptionProduct,
        userToken: String,
        redirect: IapStore,
    ): JsonObject {
        return Json.run {
            buildJsonObject {
                put("packageName", envManager.subscriptionPackageName)
                put("productId", subscriptionProduct.normalizeName)
                put("token", userToken)
                put("redirect", redirect.storeName)
            }
        }
    }

    override fun cancelSubscription(subscriptionProduct: SubscriptionProduct, userToken: String, redirect: IapStore) {
        val bodyJson = api.post(
            "${envManager.apMonetizationPath}/subscription/cancel",
            envManager.apSignatureToken,
            buildBody(subscriptionProduct, userToken, redirect)
        )
        val response = deserialize<CancelSubscriptionResponse>(bodyJson)
        if (response.statusCode != 0) {
            throw CustomException("Cancel failed, statusCode ${response.statusCode}")
        }
        if (!response.message) {
            throw CustomException("Cancel failed, message false")
        }
    }
}

@Serializable
class VerifySubscriptionResponse(
    val statusCode: Int,
    val message: VerifySubscription
)

@Serializable
class VerifySubscription(
    @SerialName("state")
    private val _state: String,
    @SerialName("startTime")
    private val _startTime: Long,
    @SerialName("endTime")
    private val _endTime: Long
) {
    @Transient
    val state = SubscriptionState.fromValue(_state)

    @Transient
    val startTime = Instant.ofEpochSecond(_startTime)

    @Transient
    val endTime = Instant.ofEpochSecond(_endTime)
}

@Serializable
class CancelSubscriptionResponse(
    val statusCode: Int,
    val message: Boolean
) 