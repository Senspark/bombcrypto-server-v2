package com.senspark.game.data.model.nft

import com.senspark.game.declare.EnumConstants.HeroType
import com.senspark.game.utils.Encoder
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class ConfigHeroTraditional(
    val itemId: Int,
    val skin: Int,
    val color: Int,
    val speed: Int,
    val range: Int,
    val bomb: Int,
    val hp: Int,
    val dmg: Int,
    val maxSpeed: Int,
    val maxRange: Int,
    val maxBomb: Int,
    val maxHp: Int,
    val maxDmg: Int,
    val maxUpgradeSpeed: Int,
    val maxUpgradeRange: Int,
    val maxUpgradeBomb: Int,
    val maxUpgradeHp: Int,
    val maxUpgradeDmg: Int,
    val tutorial: Int,
    val canBeBot: Boolean,
) {
    fun toSFSObject(): ISFSObject {
        val result = SFSObject()
        result.putInt("item_id", itemId)
        result.putLong("bomber_id", 0)
        result.putInt("status", 0)
        result.putInt("index", 0)
        result.putInt("power", 1)
        result.putInt("level", 1)
        result.putInt("rare", 0)
        result.putInt("shield_level", 0)
        result.putInt("is_reset", 0)
        result.putInt("stamina", 1)
        result.putInt("bomb_skin", 1)
        result.putInt("bomb", 1)
        result.putInt("skin", skin)
        result.putInt("color", color)
        result.putInt("speed", speed)
        result.putInt("bomb_range", range)
        result.putInt("bomb", bomb)
        result.putInt("hp", hp)
        result.putInt("dmg", dmg)
        result.putUtfString(
            "ability",
            "[]"
        )
        result.putUtfString(
            "ability_shield",
            "[]"
        )
        result.putInt("maxSpeed", maxSpeed)
        result.putInt("maxRange", maxRange)
        result.putInt("maxBomb", maxBomb)
        result.putInt("maxHp", maxHp)
        result.putInt("maxDmg", maxDmg)

        result.putInt("maxUpgradeSpeed", maxUpgradeSpeed)
        result.putInt("maxUpgradeRange", maxUpgradeRange)
        result.putInt("maxUpgradeBomb", maxUpgradeBomb)
        result.putInt("maxUpgradeHp", maxUpgradeHp)
        result.putInt("maxUpgradeDmg", maxUpgradeDmg)
        
        result.putInt("tutorial", tutorial)
        result.putInt("type", HeroType.TR.value)
        result.putUtfString("gen_id", Encoder.parseHeroGenId(result))
        return result
    }
}