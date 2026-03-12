package com.senspark.game.api.model.response

import kotlinx.serialization.Serializable


@Serializable
data class ClaimSignatureResponse(val data: ClaimSignatureResponseData)

@Serializable
data class ClaimSignatureResponseData(
    val nonce: Int,
    val signature: String,
    val amount: Double
)
