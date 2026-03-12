package com.senspark.game.utils

import com.smartfoxserver.v2.entities.data.ISFSObject
import java.math.BigInteger

object Encoder {
    fun parseHeroGenId(obj: ISFSObject): String {
        var detail: BigInteger = BigInteger.ZERO
        detail = detail.or(obj.getLong("bomber_id").toBigInteger()) //id
        detail = detail.or(obj.getInt("index").toBigInteger() shl 30)//index
        detail = detail.or(obj.getInt("rare").toBigInteger() shl 40) // rarity
        detail = detail.or(obj.getInt("level").toBigInteger() shl 45) // level
        detail = detail.or(obj.getInt("color").toBigInteger() shl 50) // color
        detail = detail.or(obj.getInt("skin").toBigInteger() shl 55) // skin
        detail = detail.or(obj.getInt("stamina").toBigInteger() shl 60) // stamina
        detail = detail.or(obj.getInt("speed").toBigInteger() shl 65) // speed
        detail = detail.or(obj.getInt("bomb_skin").toBigInteger() shl 70) // bomb_skin
        detail = detail.or(obj.getInt("bomb").toBigInteger() shl 75) // bomb
        detail = detail.or(obj.getInt("power").toBigInteger() shl 80) // power
        detail = detail.or(obj.getInt("bomb_range").toBigInteger() shl 85) // range
        val abilities = deserializeList<Int>(obj.getUtfString("ability"))
        detail = detail.or(abilities.size.toBigInteger() shl 90) // ability
        for (i in abilities.indices) {
            detail = detail.or(abilities[i].toBigInteger() shl (95 + i * 5))
        }
        if (obj.containsKey("maxSpeed")) {
            detail = detail.or(obj.getInt("maxSpeed").toBigInteger() shl 130) // range
        }
        if (obj.containsKey("maxRange")) {
            detail = detail.or(obj.getInt("maxRange").toBigInteger() shl 135) // range
        }
        if (obj.containsKey("maxBomb")) {
            detail = detail.or(obj.getInt("maxBomb").toBigInteger() shl 140) // range
        }
        val abilitiesHero = deserializeList<Int>(obj.getUtfString("ability_shield"))
        detail = detail.or(abilitiesHero.size.toBigInteger() shl 180)
        for (i in abilitiesHero.indices) {
            detail = detail.or(abilitiesHero[i].toBigInteger() shl (185 + i * 5))
        }
        detail = detail.or(obj.getInt("shield_level").toBigInteger() shl 235) // numUpgradeShield
        detail = detail.or(obj.getInt("is_reset").toBigInteger() shl 240) // numResetShield
        return detail.toString()
    }
}