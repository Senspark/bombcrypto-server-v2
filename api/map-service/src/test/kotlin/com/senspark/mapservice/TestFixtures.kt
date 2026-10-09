package com.senspark.mapservice

import com.senspark.mapservice.model.HeroSnapshotDto

fun testHero(heroId: Int = 1) = HeroSnapshotDto(
    heroId = heroId,
    bombRange = 2,
    pierceBlock = false,
    damageTreasure = 10,
    damageJail = 10,
    totalPower = 0,
    stakeBcoin = 0.0,
    stakeSen = 0.0,
    rarity = 0,
    isHeroS = false,
    dataType = "TR",
    isAirdropUser = false,
)
