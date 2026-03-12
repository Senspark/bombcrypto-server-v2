package com.senspark.game.data.model.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigInteger

@Serializable
data class UserBlockRewardGift(

    @SerialName("uid")
    val uid: Int,
    @SerialName("reward_type")
    val rewardType: String,
    @SerialName("count")
    val count: Int,
    @SerialName("rarity")
    val rarity: Int,
    @SerialName("category")
    val category: Int,
    @SerialName("is_hero_s")
    val isHeroS: Int,
    @SerialName("skin")
    val skin: Int,
    @SerialName("modify_time")
    val modifyTime: String,
    @SerialName("status")
    val status: String,
    @SerialName("drop_rates")
    val dropRates: List<Int>
) {
    fun parseToGenId(): BigInteger {
        var detail: BigInteger = BigInteger.ZERO
        detail = detail.or(count.toBigInteger()) //count
        detail = detail.or(rarity.toBigInteger().shiftLeft(10)) // rarity
        detail = detail.or(category.toBigInteger().shiftLeft(15)) // category
        detail = detail.or(isHeroS.toBigInteger().shiftLeft(20)) // isHero S
        detail = detail.or(dropRates.size.toBigInteger().shiftLeft(25)) // drop rate length
        for (i in 0..5) {
            detail = detail.or(dropRates[i].toBigInteger().shiftLeft(30 + i * 15))
        }
        detail = detail.or(skin.toBigInteger().shiftLeft(120)) // drop rate length
        return detail
    }
}
