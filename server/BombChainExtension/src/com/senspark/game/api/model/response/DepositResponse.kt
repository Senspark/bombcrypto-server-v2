package com.senspark.game.api.model.response

import kotlinx.serialization.Serializable


@Serializable
data class DepositResponse(
    val type: String,
    val value: Float
)
