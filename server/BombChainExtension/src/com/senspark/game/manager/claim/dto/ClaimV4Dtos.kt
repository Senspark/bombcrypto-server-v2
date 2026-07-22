package com.senspark.game.manager.claim.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClaimCheckResponse(
    val correlationId: String,
    val totalClaimed: Double = 0.0,
    val errorMessage: String? = null,
)

@Serializable
data class ClaimSignResponse(
    val correlationId: String,
    val nonce: Int = 0,
    val signature: String = "",
    val amount: Double = 0.0,
    val errorMessage: String? = null,
)

data class ClaimV4Prep(
    val tokenTypeInBlockChain: Int,
)

data class ClaimV4Finalize(
    val newTotalClaimed: Double,
    val giftDetails: List<String>,
    val received: Double,
)

