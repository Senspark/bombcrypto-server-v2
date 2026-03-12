@file:OptIn(ExperimentalSerializationApi::class)

package com.senspark.game.data.model.nft

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class HeroTraditional(
    val id: Int,
    val level: Int,
    @SerialName("item_id")
    val itemId: Int,
    var status: Int,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    var quantity: Int = 1
)