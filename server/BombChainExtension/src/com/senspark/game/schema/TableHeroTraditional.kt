package com.senspark.game.schema

import org.jetbrains.exposed.sql.Table

object TableHeroTraditional : Table("config_hero_traditional_config") {
    val itemId = integer("item_id")
    val skin = integer("skin")
    val color = integer("color")
    val speed = integer("speed")
    val range = integer("range")
    val bomb = integer("bomb")
    val hp = integer("hp")
    val dmg = integer("dmg")
    val maxSpeed = integer("max_speed")
    val maxRange = integer("max_range")
    val maxBomb = integer("max_bomb")
    val maxDmg = integer("max_dmg")
    val maxHp = integer("max_hp")
    val tutorial = integer("tutorial")
    val canBeBot = integer("can_be_bot")
    val maxUpgradeSpeed = integer("max_upgrade_speed")
    val maxUpgradeRange = integer("max_upgrade_range")
    val maxUpgradeBomb = integer("max_upgrade_bomb")
    val maxUpgradeDmg = integer("max_upgrade_hp")
    val maxUpgradeHp = integer("max_upgrade_dmg")
}