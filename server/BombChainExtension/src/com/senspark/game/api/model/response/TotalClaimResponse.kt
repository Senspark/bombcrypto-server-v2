package com.senspark.game.api.model.response

import kotlinx.serialization.Serializable


@Serializable
data class TotalClaimResponse(val code: Int, val message: String)
