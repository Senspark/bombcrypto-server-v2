package com.senspark.game.pvp.info

import com.senspark.common.pvp.IMatchResultHeroInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MatchResultHeroInfo(
    @SerialName("id") override val id: Int,
    @SerialName("damage_source") override val damageSource: Int,
    @SerialName("rewards") override val rewards: Map<Int, Float>,
    @SerialName("collected_items") override val collectedItems: List<Int>,
) : IMatchResultHeroInfo