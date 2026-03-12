package com.senspark.game.data.model.nft

import com.senspark.game.declare.GameConstants
import com.senspark.game.utils.serialize
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.Serializable

class HeroShield(
    var rarity: Int,
    val items: MutableMap<Int, Int>,
    private var _level: Int,
    private var _maxCapacity: Int,
    private val getMaxCapacity: (shieldLevel: Int, rarity: Int) -> Int,
) {

    var level: Int
        get() = _level
        set(value) {
            _level = value
            _maxCapacity = getMaxCapacity(value, rarity)
        }

    /**
     * Gets amount of damage that can nullify the specified ability.
     * @param ability Shield ability.
     */
    fun getCapacity(ability: Int): Int {
        return items[ability] ?: 0
    }

    fun resetCapacity(ability: Int) {
        items.computeIfPresent(ability) { _, _ ->
            _maxCapacity
        }
    }

    fun takeDamage(ability: Int, damage: Int) {
        require(items.containsKey(ability))
        items.computeIfPresent(ability) { _, value ->
            value - damage
        }
    }

    fun toSFSArray(hero: Hero): ISFSArray {
        val result: ISFSArray = SFSArray()

        if (!hero.isHeroS && !hero.isFakeS) {
            return result
        }
        val staminaDame = GameConstants.staminaDame
        items.forEach { (ability, capacity) ->
            val obj = SFSObject.newInstance()
            var stamina = capacity / staminaDame
            val remainder = capacity % staminaDame
            if (remainder >= hero.totalPower + hero.damageTreasure) {
                ++stamina
            }
            obj.putInt("ability", ability)
            obj.putInt("current", stamina)
            obj.putInt("total", _maxCapacity / staminaDame)
            result.addSFSObject(obj)
        }
        return result
    }

    override fun toString(): String {
        return items.map {
            Data(it.key, it.value)
        }.serialize()
    }

    @Serializable
    class Data(val ability: Int, val finalDamage: Int)
}