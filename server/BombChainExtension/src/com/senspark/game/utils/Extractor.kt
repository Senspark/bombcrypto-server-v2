package com.senspark.game.utils

import com.senspark.game.declare.EnumConstants.HeroType
import com.smartfoxserver.v2.entities.data.ISFSObject
import java.time.Instant

object Extractor {
    fun parseHeroId(heroId: Int, heroType: HeroType): Int {
        return heroId * 10 + heroType.value
    }

    inline fun <reified T> tryGet(obj: ISFSObject, key: String, defaultValue: T): T {
        if (obj.containsKey(key) && !obj.isNull(key)) {
            return when (T::class) {
                Int::class -> obj.getInt(key) as T
                Long::class -> obj.getLong(key) as T
                String::class -> obj.getUtfString(key) as T
                Instant::class -> Instant.ofEpochMilli(obj.getLong(key)) as T
                // Add more types as needed
                else -> defaultValue
            }
        }
        return defaultValue
    }
}