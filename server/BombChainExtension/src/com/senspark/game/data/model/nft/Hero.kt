package com.senspark.game.data.model.nft

import com.senspark.game.data.manager.hero.*
import com.senspark.game.declare.GameConstants
import com.senspark.game.declare.GameConstants.BOMBER_ABILITY
import com.senspark.game.declare.SFSField
import com.senspark.game.utils.Utils
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class Hero(
    val userId: Int,
    private var _details: IHeroDetails,
    private var _active: Boolean,
    private var _stage: Int,
    private var _energy: Int,
    private var _timeRest: Long,
    private var _shield: HeroShield,
    private var _stakeBcoin: Double,
    private var _stakeSen: Double,
    private var _lockSince: Instant,
    private var _lockSeconds: Int,
    private val _helper: IHeroHelper,
) {

    private var _usedEnergy = 0
    val details get() = _details

    val heroId get() = _details.heroId
    val rarity = _details.rarity
    val level = _details.level
    val color = _details.color
    val skin = _details.skin
    val stamina = _details.stamina
    val bombSkin = _details.bombSkin
    val bombCount = _details.bombCount
    val bombRange = _details.bombRange
    val speed = _details.speed
    val hp = _details.hp
    val dmg = _details.dmg
    val bombPower = _details.bombPower
    val abilityList = _details.abilityList
    val abilityHeroSList = _details.abilityHeroSList
    val resetShieldCounter = _details.resetShieldCounter
    val type = _details.type
    val maxSpeed = _details.maxSpeed
    val maxBomb = _details.maxBomb
    val maxRange = _details.maxRange
    val maxHp = _details.maxHp
    val maxDmg = _details.maxDmg
    val maxUpgradeSpeed = _details.maxUpgradeSpeed
    val maxUpgradeRange = _details.maxUpgradeRange
    val maxUpgradeBomb = _details.maxUpgradeBomb
    val maxUpgradeHp = _details.maxUpgradeHp
    val maxUpgradeDmg = _details.maxUpgradeDmg
    val heroTrType = _details.heroTRType
    val upgradedBomb = _details.upgradedBomb
    val upgradedRange = _details.upgradedRange
    val upgradedSpeed = _details.upgradedSpeed
    val upgradedHp = _details.upgradedHp
    val upgradedDmg = _details.upgradedDmg
    val heroConfig = _details.heroConfig
    val isHeroS = _details.isHeroS()
    val status = if (_details is NonFiHeroDetails) (_details as NonFiHeroDetails).status else 1
    var lockUntil = if (_lockSeconds > 0) _lockSince.plusSeconds(_lockSeconds.toLong()) else null
    val isLocked get() = lockUntil != null && lockUntil!!.isAfter(Instant.now())

    /** Damage to treasure chests. */
    val damageTreasure = _helper.getDamageTreasure(this)

    /** Damage to jail chests. */
    val damageJail = _helper.getDamageJail(this)

    val totalPower = _helper.getTotalPower(this)

    val totalProperties get() = totalPower + bombRange + stamina + speed + bombCount + abilityList.items.size

    var isActive
        get() = _active
        set(value) {
            _active = value
        }

    var stage
        get() = _stage
        set(value) {
            _stage = value
        }

    val energy get() = _energy

    var timeRest
        get() = _timeRest
        set(value) {
            _timeRest = value
        }

    var stakeBcoin
        get() = _stakeBcoin
        set(value) {
            _stakeBcoin = value
        }


    var stakeSen
        get() = _stakeSen
        set(value) {
            _stakeSen = value
        }

    val isFakeS get() = _helper.isFakeS(this)
    
    private val percentSaveEnergy = _helper.getPercentSaveEnergy(this)

    var hashBombExplode: MutableMap<Int, Long> = ConcurrentHashMap()
        private set
    
    val shield get() = _shield

    fun onSaved() {
        _usedEnergy = 0
    }

    fun addEnergy(value: Int): Int {
        val maxEnergy = stamina * 50
        val energyRecovery = min(value, maxEnergy - energy)
        _energy += energyRecovery
        return energyRecovery
    }

    fun updateDetails(details: IHeroDetails) {
        this._details = details
    }

    fun subEnergy() {
        //kiem tra xem bomber co skill so 4: 30% không mất năng lượng ko
        var isSaveEnergy = false
        if (percentSaveEnergy > 0) {
            val randNumber = Utils.randInt(0, 100)
            isSaveEnergy = randNumber <= percentSaveEnergy
        }
        if (isSaveEnergy) {
            // No change.
        } else {
            _energy -= 1
            _usedEnergy += 1
            //TODO cap nhat thoi gian lan cuoi trừ energy để tính.
        }
    }

    //neu the luc < 6 thi 80% bi set danh, nguoc lai 50%
    val isDangerous: Boolean
        get() =//neu the luc < 6 thi 80% bi set danh, nguoc lai 50%
            if (energy < GameConstants.ENERGY_DANGEROUS) calculatorPercentDangerous(80) else calculatorPercentDangerous(
                50
            )

    fun killBomberman() {
        _usedEnergy += _energy
        _energy = 0
        //TODO cap nhat thoi gian lan cuoi trừ energy để tính.
    }

    fun isAvoid(isDamTreasure: Boolean): Boolean {
        //TODO neu co them ky nang tranh nguy hiem khac thi them
        return if (isHeroS || isFakeS) {
            getFinalDamageShield(BOMBER_ABILITY.AVOID_THUNDER) >= getTotalPowerDamage(isDamTreasure)
        } else false
    }

    private fun getTotalPowerDamage(isDamTreasure: Boolean): Int {
        return totalPower + if (isDamTreasure) damageTreasure else 0
    }

    fun getFinalDamageShield(ability: Int): Int {
        return shield.getCapacity(ability)
    }

    fun subStaminaShield(isDamTreasure: Boolean) {
        if (isHeroS || isFakeS) {
            val finalDamageShield = getFinalDamageShield(BOMBER_ABILITY.AVOID_THUNDER)
            if (finalDamageShield >= damageTreasure + totalPower) {
                shield.takeDamage(BOMBER_ABILITY.AVOID_THUNDER, getTotalPowerDamage(isDamTreasure))
            }
        }
    }

    fun resetShieldToFull(ability: Int) {
        shield.resetCapacity(ability)
    }

    fun calculatorPercentDangerous(percent: Int): Boolean {
        val randNumber = Utils.randInt(0, 100)
        return randNumber <= percent
    }

    fun containsAbility(ability: Int): Boolean {
        return abilityList.has(ability)
    }

    fun toSFSObject(): ISFSObject {
        val obj = SFSObject()
        obj.putInt("playerType", skin)
        obj.putInt("playercolor", color)
        obj.putUtfString("genId", _details.details)
        obj.putLong("id", heroId.toLong())
        obj.putInt("bombDamage", bombPower)
        obj.putInt("speed", speed)
        obj.putInt("stamina", stamina)
        obj.putInt("bombNum", bombCount)
        obj.putInt("bombRange", bombRange)
        obj.putInt("bombSkin", bombSkin)
        obj.putInt("energy", energy)
        obj.putInt("level", level)
        obj.putInt("rare", rarity)
        obj.putInt("maxHp", stamina)
        obj.putInt("stage", stage)
        obj.putInt("active", if (isActive) 1 else 0)
        obj.putIntArray("abilities", abilityList.items)
        obj.putSFSArray("shields", shield.toSFSArray(this))
        obj.putInt("maxSpeed", maxSpeed)
        obj.putInt("maxRange", maxRange)
        obj.putInt("maxBomb", maxBomb)
        obj.putInt("max_upgrade_speed", maxUpgradeSpeed)
        obj.putInt("max_upgrade_range", maxUpgradeRange)
        obj.putInt("max_upgrade_bomb", maxUpgradeBomb)
        obj.putInt("max_upgrade_hp", maxUpgradeHp)
        obj.putInt("max_upgrade_dmg", maxUpgradeDmg)
        obj.putLong("lock_since", _lockSince.toEpochMilli())
        obj.putInt("lock_seconds", _lockSeconds)
        obj.putInt(SFSField.HeroType, type.value)
        return obj
    }

    fun updateShieldLevel(level: Int) {
        shield.level = level
    }

    fun updateLockUntil(time: Instant?) {
        lockUntil = time
    }

    /**
     * Tạo shield level 0
     */
    fun addBasicShield() {
        _shield = _helper.heroShieldBuilder.create(rarity)
    }
    
    fun addShield(data:String) {
        _shield = _helper.heroShieldBuilder.fromString(rarity, data, _shield.level)
    }
}