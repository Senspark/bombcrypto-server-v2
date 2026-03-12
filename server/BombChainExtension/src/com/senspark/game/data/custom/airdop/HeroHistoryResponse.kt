package com.senspark.game.data.custom.airdop

import kotlinx.serialization.Serializable

@Serializable
class HeroHistoryResponse(
    val message: List<BombermanBuyTime>
)