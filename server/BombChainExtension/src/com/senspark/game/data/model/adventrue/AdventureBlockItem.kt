package com.senspark.game.data.model.adventrue

import com.senspark.common.constant.PvPItemType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class AdventureBlockItem(
    val i: Int,
    val j: Int,
    @Transient
    val itemType: PvPItemType = PvPItemType.Default,
    @SerialName("item")
    val itemTypeValue: Int,
    var rewardValue: Int
)