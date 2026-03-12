package com.senspark.game.user

import com.senspark.common.constant.PvPItemType
import com.senspark.game.constant.Booster
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.declare.GameConstants
import com.senspark.game.declare.SFSField
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlin.math.min

class AdventureHero(
    override val hero: Hero,
    calculator: IInitHeroStatCalculator
) : IAdventureHero {

    private var _hp: Int
    private var _baseHp: Int
    private var _range: Int
    private var _dmg: Int
    private var _speed: Int
    private var _bomb: Int
    private var _boosters: Set<Booster> = HashSet()
    private var _itemsEffected = HashMap<PvPItemType, Int>()
    override val heroId: Int = hero.heroId

    init {
        _hp = calculator.hp
        _baseHp = hp
        _range = calculator.range
        _dmg = calculator.dmg
        _speed = calculator.speed
        _bomb = calculator.bomb
    }


    override val speed: Int
        get() {
            return min(hero.maxSpeed, _speed)
        }

    override val dmg: Int
        get() {
            return _dmg
        }

    override val hp: Int
        get() {
            return _hp
        }

    override val bomb: Int
        get() {
            return min(hero.maxBomb, _bomb)
        }

    override val range: Int
        get() {
            val itemValue = _itemsEffected[PvPItemType.FireUp] ?: 0
            val newRange = _range + itemValue
            return min(hero.maxRange, newRange)
        }

    override fun subHealth(value: Int) {
        _hp -= value
    }

    override fun revive() {
        _hp = min(_baseHp, 3)
    }

    override fun applyBooster(boosters: Set<Booster>) {
        _boosters = boosters
    }

    override fun takeItem(item: PvPItemType) {
        _itemsEffected[item] = (_itemsEffected[item] ?: 0) + 1
    }

    override fun canPierceBlock(): Boolean {
        return hero.containsAbility(GameConstants.BOMBER_ABILITY.PIERCE_BLOCK)
    }

    override fun toSFSObject(): ISFSObject {
        val obj = SFSObject()
        obj.putInt("playerType", hero.skin)
        obj.putInt("playercolor", hero.color)
        obj.putUtfString("genId", hero.details.details)
        obj.putLong("id", hero.heroId.toLong())
        obj.putInt("bombDamage", dmg)
        obj.putInt("speed", speed)
        obj.putInt("hp", hp)
        obj.putInt("stamina", hero.stamina)
        obj.putInt("bombNum", bomb)
        obj.putInt("bombRange", range)
        obj.putInt("bombSkin", hero.bombSkin)
        obj.putInt("energy", hero.energy)
        obj.putInt("level", hero.level)
        obj.putInt("rare", hero.rarity)
        obj.putInt("stage", hero.stage)
        obj.putInt("active", if (hero.isActive) 1 else 0)
        obj.putIntArray("abilities", hero.abilityList.items)
        obj.putSFSArray("shields", hero.shield.toSFSArray(hero))
        obj.putInt("maxSpeed", hero.maxSpeed)
        obj.putInt("maxRange", hero.maxRange)
        obj.putInt("maxBomb", hero.maxBomb)
        obj.putInt("maxHp", hero.maxHp)
        obj.putInt("maxDmg", hero.maxDmg)
        obj.putInt(SFSField.HeroType, hero.type.value)
        return obj
    }
}