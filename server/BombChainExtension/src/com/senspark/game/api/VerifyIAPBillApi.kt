package com.senspark.game.api

import com.senspark.common.utils.ILogger
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.IEnvManager
import kotlinx.serialization.json.*

interface IVerifyIapBillResult {
    val isTest: Boolean
    val region: String
}

class VerifyIAPBillApi(
    envManager: IEnvManager,
    private val _api: IRestApi,
    private val _logger: ILogger,
) : IVerifyIAPBillApi {
    private val _authorizationToken = envManager.apLoginToken
    private val _url = "${envManager.apMonetizationPath}/iap/purchase/verify"

    init {
        _logger.log("VerifyIAPBillApi initialized with URL: $_url")
    }

    override fun verify(
        packageName: String,
        productId: String,
        billToken: String,
        transactionId: String,
        redirect: String
    ): IVerifyIapBillResult {
        val body = Json.run {
            buildJsonObject {
                put("packageName", packageName)
                put("productId", productId)
                put("token", billToken)
                put("transactionId", transactionId)
                put("redirect", redirect)
            }
        }
        _logger.log("nhanc19 verify bill: $packageName, $productId, $billToken, $transactionId, $redirect")
        val responseBody = _api.post(
            _url,
            _authorizationToken,
            body
        )
        val message = Json.parseToJsonElement(responseBody).jsonObject["message"]
            ?: throw CustomException("Invalid response: message not found")
        val purchased = message.jsonObject["purchased"].toString().toBooleanStrictOrNull()
            ?: throw CustomException("Verify bill failed, message isn't boolean")
        _logger.log("nhanc19 verify bill: $responseBody, $purchased")
        require(purchased) {
            "Verify bill failed, bill invalid"
        }

        return object : IVerifyIapBillResult {
            override val isTest = message.jsonObject["is_test"].toString().toBooleanStrictOrNull() ?: false
            override val region = message.jsonObject["region"]?.jsonPrimitive?.content ?: ""
        }

    }
}