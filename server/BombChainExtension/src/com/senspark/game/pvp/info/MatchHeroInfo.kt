package com.senspark.game.pvp.info

import com.senspark.common.constant.ItemId
import com.senspark.common.pvp.IMatchHeroInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MatchHeroInfo(
    @SerialName("id") override val id: Int,
    @SerialName("color") override val color: Int,
    @SerialName("skin") override val skin: Int,
    @SerialName("skin_chests") override val skinChests: Map<Int, List<ItemId>>,
    @SerialName("health") override val health: Int,
    @SerialName("speed") override val speed: Int,
    @SerialName("damage") override val damage: Int,
    @SerialName("bomb_count") override val bombCount: Int,
    @SerialName("bomb_range") override val bombRange: Int,
    @SerialName("max_health") override val maxHealth: Int,
    @SerialName("max_speed") override val maxSpeed: Int,
    @SerialName("max_damage") override val maxDamage: Int,
    @SerialName("max_bomb_count") override val maxBombCount: Int,
    @SerialName("max_bomb_range") override val maxBombRange: Int,
) : IMatchHeroInfo {
    override fun toString(): String {
        val builder = StringBuilder()
            .append("$id-$color-$skin-${skinChests.toSortedMap()}-")
            .append("$health-$speed-$damage-$bombCount-$bombRange-")
            .append("$maxHealth-$maxSpeed-$maxDamage-$maxBombCount-$maxBombRange-")
        return builder.toString()
    }
}
