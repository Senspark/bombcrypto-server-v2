package com.senspark.game.schema

import org.jetbrains.exposed.sql.Table

object TableHeroTrial : Table("config_hero_trial_config") {
    val bombPower = varchar("power", 255)
    val bombRange = varchar("bomb_range", 255)
    val stamina = varchar("stamina", 255)
    val speed = varchar("speed", 255)
    val bombCount = varchar("bomb", 255)
    val ability = varchar("ability", 255)
    val skin = varchar("charactor", 255)
    val color = varchar("color", 255)
    val rarity = integer("rare")
    val number = integer("number")
    val bombSkin = varchar("bomb_skin", 255)
    val abilityHero = varchar("ability_shield", 255)
}