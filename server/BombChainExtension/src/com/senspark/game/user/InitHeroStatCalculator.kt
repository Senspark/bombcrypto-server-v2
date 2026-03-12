package com.senspark.game.user

import com.senspark.common.constant.ItemId
import com.senspark.game.constant.Booster
import com.senspark.game.constant.ItemType
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.model.nft.Hero
import kotlin.math.min


class InitHeroStatCalculator(
    configItemManager: IConfigItemManager,
    itemWearing: Map<ItemType, List<ItemId>>,
    boosters: Set<Booster>,
    hero: Hero,
) : IInitHeroStatCalculator {

    override var hp = hero.hp
        private set
    override var dmg = hero.dmg
        private set
    override var range = hero.bombRange
        private set
    override var speed = hero.speed
        private set
    override var bomb = hero.bombCount
        private set

    init {
        itemWearing.map { it.value }.flatten().forEach {
            val item = configItemManager.getItem(it)
            hp += item.hpBonus
            dmg += item.dmgBonus
            range += item.rangeBonus
            speed += item.speedBonus
            bomb += item.bombBonus
        }


        //cộng booster
        speed += if (boosters.contains(Booster.SpeedPlusOne)) 1 else 0
        range += if (boosters.contains(Booster.RangePlusOne)) 1 else 0
        bomb += if (boosters.contains(Booster.BombPlusOne)) 1 else 0

        hp = min(hp, hero.maxHp)
        speed = min(speed, hero.maxSpeed)
        dmg = min(dmg, hero.maxDmg)
        bomb = min(bomb, hero.maxBomb)
        range = min(range, hero.maxRange)
    }
}