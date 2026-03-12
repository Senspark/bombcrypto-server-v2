package com.senspark.game.pvp.data

import com.senspark.common.constant.ItemId
import com.senspark.common.pvp.IMatchHeroInfo

class CustomMatchHeroInfo(
    info: IMatchHeroInfo,
    skinChests: Map<Int, List<ItemId>>?,
    health: Int?,
    speed: Int?,
    damage: Int?,
    bombCount: Int?,
    bombRange: Int?,
    maxHealth: Int?,
    maxSpeed: Int?,
    maxDamage: Int?,
    maxBombCount: Int?,
    maxBombRange: Int?,
) : IMatchHeroInfo {
    override val id = info.id
    override val color = info.color
    override val skin = info.skin
    override val skinChests: Map<Int, List<ItemId>> = skinChests ?: info.skinChests
    override val health = health ?: info.health
    override val speed = speed ?: info.speed
    override val damage = damage ?: info.damage
    override val bombCount = bombCount ?: info.bombCount
    override val bombRange = bombRange ?: info.bombRange
    override val maxHealth = maxHealth ?: info.maxHealth
    override val maxSpeed = maxSpeed ?: info.maxSpeed
    override val maxDamage = maxDamage ?: info.maxDamage
    override val maxBombCount = maxBombCount ?: info.maxBombCount
    override val maxBombRange = maxBombRange ?: info.maxBombRange
}