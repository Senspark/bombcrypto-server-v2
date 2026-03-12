package com.senspark.game.data.model.nft

import com.senspark.game.declare.GameConstants
import com.senspark.game.service.IHeroUpgradeShieldManager
import com.senspark.game.utils.deserializeList
import kotlinx.serialization.Serializable

class HeroShieldBuilder(
    private val _heroUpgradeShieldManager: IHeroUpgradeShieldManager
) : IHeroShieldBuilder {

    override fun create(rarity: Int): HeroShield {
        // force add shield to hero with level 0 and item [1]
        val shieldLevel = 0
        val capacity = getMaxCapacity(shieldLevel, rarity)
        val items = mutableMapOf<Int, Int>()
        items[1] = capacity
        return HeroShield(rarity, items, shieldLevel, capacity, ::getMaxCapacity)
    }

    override fun create(details: IHeroDetails): HeroShield {
        val capacity = getMaxCapacity(details.shieldLevel, details.rarity)
        val items = mutableMapOf<Int, Int>()
        details.abilityHeroSList.items.forEach {
            when (it) {
                GameConstants.BOMBER_ABILITY.AVOID_THUNDER -> items[it] = capacity
                else -> throw IllegalArgumentException("Invalid ability: $it")
            }
        }
        return HeroShield(details.rarity, items, details.shieldLevel, capacity, ::getMaxCapacity)
    }

    override fun fromString(rarity: Int, str: String, shieldLevel: Int): HeroShield {
        val items = deserializeList<Data>(str).associate { Pair(it.ability, it.finalDamage) }.toMutableMap()
        val capacity = getMaxCapacity(shieldLevel, rarity)
        return HeroShield(rarity, items, shieldLevel, capacity, ::getMaxCapacity)
    }

    override fun getMaxCapacity(shieldLevel: Int, rarity: Int): Int {
        return _heroUpgradeShieldManager.getValue(shieldLevel, rarity) * GameConstants.staminaDame
    }

    @Serializable
    class Data(val ability: Int, val finalDamage: Int)
}