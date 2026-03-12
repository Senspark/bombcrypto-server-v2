package com.senspark.game.api.model.request

import kotlinx.serialization.Serializable


@Serializable
data class ClaimSignatureRequest(
    val userAddress: String,
    val tokenType: Int,
    val amount: Double,
    val details: List<String>,
    val network: String
)