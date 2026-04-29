package com.senspark.game.pvp.entity

import com.senspark.common.pvp.IMatchHeroInfo
import com.senspark.common.utils.ILogger
import com.senspark.game.constant.Booster
import com.senspark.game.pvp.component.ComponentContainer
import com.senspark.game.pvp.component.IEntityComponent
import com.senspark.game.pvp.component.StateComponent
import com.senspark.game.pvp.config.IHeroConfig
import com.senspark.game.pvp.manager.IBombManager
import com.senspark.game.pvp.manager.IEntityManager
import com.senspark.game.pvp.manager.ITimeManager
import com.senspark.game.pvp.strategy.position.InterpolationPositionStrategy
import com.senspark.game.pvp.user.IParticipantController
import com.senspark.game.pvp.utility.IRandom
import com.senspark.game.pvp.utility.WeightedRandomizer
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

private interface IEffectManager {
    fun getState(effect: HeroEffect): IHeroEffectState
    fun push(effect: HeroEffect, duration: Int, reason: HeroEffectReason)
    fun pop(effect: HeroEffect, reason: HeroEffectReason)
    fun step(delta: Int, onTimeOut: (effect: HeroEffect) -> Unit)
}

private fun IEffectManager.isActive(effect: HeroEffect): Boolean {
    return getState(effect).isActive
}

private class EffectManager(
    initialState: Map<HeroEffect, IHeroEffectState>,
    private val _timeManager: ITimeManager,
) : IEffectManager {
    private val _states = initialState
        .filter { (_, state) -> state.isActive }
        .mapValues { (_, state) -> state.timestamp to state.duration }
        .toMutableMap()

    private val _reasons = initialState
        .mapValues { (_, state) -> state.reason }
        .toMutableMap()

    override fun getState(effect: HeroEffect): IHeroEffectState {
        val state = _states.getOrElse(effect) { 0 to 0 }
        return HeroEffectState(
            // Time-out effects (negative duration) must be popped manually.
            isActive = _states.contains(effect),
            reason = _reasons.getOrDefault(effect, HeroEffectReason.Null),
            timestamp = state.first,
            duration = state.second,
        )
    }

    override fun push(effect: HeroEffect, duration: Int, reason: HeroEffectReason) {
        _states[effect] = _timeManager.timestamp.toInt() to duration
        _reasons[effect] = reason
    }


    override fun pop(effect: HeroEffect, reason: HeroEffectReason) {
        _states.remove(effect)
        _reasons[effect] = reason
    }

    override fun step(delta: Int, onTimeOut: (effect: HeroEffect) -> Unit) {
        val now = _timeManager.timestamp.toInt()
        // Must clone to prevent java.util.ConcurrentModificationException.
        val effects = _states.keys.toList()
        effects.forEach {
            val (timestamp, duration) = _states[it] ?: return@forEach
            if (timestamp + duration > now) {
                // OK.
            } else {
                onTimeOut(it)
            }
        }
    }
}

class Hero(
    override val slot: Int,
    override val teamId: Int,
    initialState: IHeroState,
    private val _controller: IParticipantController,
    private val _config: IHeroConfig,
    private val _info: IMatchHeroInfo,
    private val _logger: ILogger,
    private val _bombManager: IBombManager,
    private val _timeManager: ITimeManager,
    private val _random: IRandom,
    private val _listener: IHeroListener,
) : IHero {
    private val _componentContainer = ComponentContainer()
    private val _positionStrategy = InterpolationPositionStrategy(500, 10, 0)
    private val _effectManager: IEffectManager

    private var _entityManager: IEntityManager? = null
    private var _alive = initialState.isAlive
    private var _health: Int
    private var _damageSource: HeroDamageSource
    private var _x: Float
    private var _y: Float
    private var _direction: Direction

    private val speed: Int
        get() {
            if (_effectManager.isActive(HeroEffect.SpeedTo1)) {
                return 1
            }
            if (_effectManager.isActive(HeroEffect.SpeedTo10)) {
                return 10
            }
            val items = items.getOrDefault(HeroItem.Boots, 0)
            return min(_info.speed + items, _info.maxSpeed)
        }

    /** Movement speed: blocks/second. */
    private val movementSpeed get() = 1.5f + speed * 0.5f

    private val bombRange: Int
        get() {
            val items = items.getOrDefault(HeroItem.FireUp, 0)
            return min(_info.bombRange + items, _info.maxBombRange)
        }
    private val bombCount: Int
        get() {
            val items = items.getOrDefault(HeroItem.BombUp, 0)
            return min(_info.bombCount + items, _info.maxBombCount)
        }

    /** Status. */
    private val shielded get() = isAlive && _effectManager.isActive(HeroEffect.Shield)
    private val invincible get() = isAlive && _effectManager.isActive(HeroEffect.Invincible)
    private val imprisoned get() = isAlive && _effectManager.isActive(HeroEffect.Imprisoned)

    override val entityManager get() = _entityManager
    override val state: IHeroState
        get() = HeroState(
            isAlive = isAlive,
            x = _x,
            y = _y,
            direction = _direction,
            health = _health,
            damageSource = _damageSource,
            items = items.toMap(), // Must clone.
            effects = listOf(
                HeroEffect.Shield,
                HeroEffect.Invincible,
                HeroEffect.Imprisoned,
                HeroEffect.SpeedTo1,
                HeroEffect.SpeedTo10,
                HeroEffect.ReverseDirection,
                HeroEffect.PlantBombRepeatedly
            ).associateWith {
                _effectManager.getState(it)
            },
        )
    override val isAlive get() = _alive
    override val damageSource: HeroDamageSource get() = _damageSource
    override val items: MutableMap<HeroItem, Int>
    override val collectedItems = mutableListOf<Int>()
    override val x get() = _x
    override val y get() = _y
    override val direction get() = _direction

    init {
        val baseState = initialState.baseState
        val positionState = initialState.positionState
        require(baseState != null && positionState != null) { "Invalid state" }
        _effectManager = EffectManager(baseState.effects, _timeManager)
        _health = baseState.health
        _damageSource = baseState.damageSource
        items = baseState.items.toMutableMap()
        _x = positionState.x
        _y = positionState.y
        _direction = positionState.direction
        _positionStrategy.addPosition(_timeManager.timestamp.toInt(), _x, _y)
        val stateComponent = StateComponent(this) { state }
        _componentContainer.addComponent(stateComponent)
    }

    override fun <T : IEntityComponent> getComponent(clazz: KClass<T>): T? {
        return _componentContainer.getComponent(clazz)
    }

    override fun applyState(state: IHeroState) {
        _alive = state.isAlive
        val baseState = state.baseState
        if (baseState != null) {
            val health = _health
            _health = baseState.health
            if (health != _health) {
                _listener.onHealthChanged(this, _health, health)
            }
            val damageSource = _damageSource
            _damageSource = baseState.damageSource
            if (health != _health || damageSource != _damageSource) {
                _listener.onDamaged(this, health - _health, _damageSource)
            }
            baseState.items.forEach { (item, value) ->
                val oldValue = items[item] ?: 0
                if (oldValue == value) {
                    return
                }
                items[item] = value
                _listener.onItemChanged(this, item, value, oldValue)
            }
            baseState.effects.forEach { (effect, effectState) ->
                if (effectState.isActive) {
                    pushEffect(effect, effectState.reason, effectState.duration)
                } else {
                    popEffect(effect, effectState.reason)
                }
            }
        }
        val positionState = state.positionState
        if (positionState != null) {
            _x = positionState.x
            _y = positionState.y
            _direction = positionState.direction
            _listener.onMoved(this, _x, _y)
        }
    }

    private var _deathTimestamp: Long = 0
    override val deathTimestamp get() = _deathTimestamp

    override fun kill() {
        _alive = false
        _deathTimestamp = _timeManager.timestamp
    }

    override fun move(timestamp: Int, x: Float, y: Float) {
        require(isAlive) { "Cannot move when not alive [slot=$slot]" }
        require(!imprisoned) { "Cannot move when imprisoned [slot=$slot]" }
        _positionStrategy.addPosition(timestamp, x, y)
        val deltaX = x - _x
        val deltaY = y - _y
        val direction = when {
            deltaX < 0 -> Direction.Left
            deltaX > 0 -> Direction.Right
            deltaY < 0 -> Direction.Down
            deltaY > 0 -> Direction.Up
            else -> _direction // Keep old direction.
        }
        _x = x
        _y = y
        _direction = direction
        _listener.onMoved(this, x, y)
    }

    override fun plantBomb(timestamp: Int, byHero: Boolean): IBomb {
        require(isAlive) { "Cannot plant bomb when not alive [slot=$slot]" }
        require(!imprisoned) { "Cannot plant bomb when imprisoned [slot=$slot]" }
        val bombs = _bombManager.getBombs(slot)
        require(bombs.size < bombCount) {
            "Maximum amount of planted bombs reached [slot=$slot]"
        }
        val (x, y) = _positionStrategy.getPosition(timestamp)
        val state = BombState(
            isAlive = true,
            slot = slot,
            reason = if (byHero) BombReason.Planted else BombReason.PlantedBySkull,
            x = x,
            y = y,
            range = bombRange,
            damage = _info.damage,
            piercing = false,
            explodeDuration = _config.explodeDuration,
            explodeRanges = emptyMap(), // Currently explode ranges will be recalculated everytime.
            plantTimestamp = _timeManager.timestamp.toInt(),
        )
        return _bombManager.plantBomb(state)
    }

    override fun damageBomb(amount: Int) {
        if (!isAlive) {
            // Already dead.
            return
        }
        if (invincible || imprisoned) {
            // No effect.
            return
        }
        if (shielded) {
            // Shield can protect from damage once.
            disableShield(HeroEffectReason.Damaged)

            // Be invincible after shield broken.
            enableInvincible()
            return
        }

        // Update damaged.
        val health = _health
        require(health > 0) { "Expected health > 0" }
        _health = max(0, _health - amount)
        _listener.onHealthChanged(this, _health, health)
        _damageSource = HeroDamageSource.Bomb

        // Dispatch events.
        _listener.onDamaged(this, amount, _damageSource)

        if (_health > 0) {
            // After taking damage, be invincible for a short duration.
            enableInvincible()
        } else {
            // After taking damage and no health left, be imprisoned for a short duration.
            enableImprisoned()

            // Remove all skull effects.
            disableSkullEffects()
        }
    }

    override fun damagePrison() {
        endImprisoned(HeroEffectReason.Damaged)
    }

    override fun rescuePrison() {
        endImprisoned(HeroEffectReason.Rescue)
    }

    private fun endImprisoned(reason: HeroEffectReason) {
        if (!isAlive) {
            // Already dead.
            return
        }
        if (!imprisoned) {
            // No effect.
            return
        }
        // Break prison.
        disableImprisoned(reason)

        when (reason) {
            HeroEffectReason.UseBooster,
            HeroEffectReason.Rescue -> {
                val health = _health
                require(health == 0) { "Expected health = 0" }
                // Max 3 health on restoration.
                _health = min(_info.health, 3)
                _listener.onHealthChanged(this, _health, health)

                // Be invincible after breaking jail.
                enableInvincible()
            }

            HeroEffectReason.TimeOut,
            HeroEffectReason.Damaged -> {
                // Update damage.
                _damageSource = HeroDamageSource.PrisonBreak
                _listener.onDamaged(this, 0, _damageSource)
                // Dead.
                kill()
            }

            else -> require(false) { "Invalid imprison reason" }
        }
    }

    override fun damageFallingBlock() {
        if (!isAlive) {
            // Already dead.
            return
        }
        // Update damage.
        val health = _health
        _health = 0
        _listener.onHealthChanged(this, _health, health)
        _damageSource = HeroDamageSource.HardBlock

        // Dispatch events.
        _listener.onDamaged(this, health, _damageSource)

        // Dead immediately.
        kill()
    }

    override fun useBooster(booster: Booster) {
        require(isAlive) { "Cannot use booster when not alive" }
        if (imprisoned) {
            require(booster == Booster.Key) { "Can only use key when imprisoned" }
        } else {
            require(booster != Booster.Key) { "Can use any items except key when not imprisoned" }
        }
        _controller.useBooster(booster)
        _logger.log("[Hero:useBooster] slot=$slot booster=$booster")
        when (booster) {
            Booster.Shield -> enableShield(HeroEffectReason.UseBooster)
            Booster.Key -> endImprisoned(HeroEffectReason.UseBooster)
            else -> throw Exception("Invalid item type: $booster")
        }
    }

    override fun takeItem(blockType: BlockType) {
        when (blockType) {
            // Handle booster items.
            BlockType.Kick -> {
                // FIXME.
            }

            BlockType.Shield -> enableShield(HeroEffectReason.TakeItem)
            BlockType.Skull -> {
                disableSkullEffects()
                enableSkullEffects()
            }

            else -> {
                // Normal items.
                val (item, amount) = when (blockType) {
                    BlockType.BombUp -> (HeroItem.BombUp to 1)
                    BlockType.FireUp -> (HeroItem.FireUp to 1)
                    BlockType.Boots -> (HeroItem.Boots to 1)
                    BlockType.GoldX1 -> {
                        (HeroItem.Gold to _random.randomInt(1, 3 + 1))
                    }

                    BlockType.GoldX5 -> {
                        val value = _random.randomInt(4, 6 + 1)
                        (HeroItem.Gold to value)
                    }

                    BlockType.BronzeChest -> (HeroItem.BronzeChest to 1)
                    BlockType.SilverChest -> (HeroItem.SilverChest to 1)
                    BlockType.GoldChest -> (HeroItem.GoldChest to 1)
                    BlockType.PlatinumChest -> (HeroItem.PlatinumChest to 1)
                    else -> {
                        require(false) { "Invalid block type: $blockType" }
                        (HeroItem.BombUp to 0) // Dummy value.
                    }
                }
                val oldValue = items[item] ?: 0
                items.merge(item, amount, Int::plus)
                _listener.onItemChanged(this, item, oldValue + amount, oldValue)
            }
        }
        collectedItems.add(blockType.ordinal)
    }

    private fun pushEffect(effect: HeroEffect, reason: HeroEffectReason, duration: Int) {
        val state = _effectManager.getState(effect)
        _effectManager.push(effect, duration, reason)
        if (!state.isActive || state.reason != reason) {
            // Effect is not active/or already active (e.g. use booster then take item) -> active.
            _listener.onEffectBegan(this, effect, reason, duration)
        }
    }

    private fun popEffect(effect: HeroEffect, reason: HeroEffectReason) {
        val state = _effectManager.getState(effect)
        _effectManager.pop(effect, reason)
        if (state.isActive || state.reason != reason) {
            _listener.onEffectEnded(this, effect, reason)
        }
    }

    /** Enables shield effect for the specified duration amount. */
    private fun enableShield(reason: HeroEffectReason) {
        require(isAlive) { "Cannot be shielded when not alive [slot=$slot]" }
        pushEffect(HeroEffect.Shield, reason, _config.shieldedDuration)
    }

    /** Immediately disables the current shield effect. */
    private fun disableShield(reason: HeroEffectReason) {
        require(shielded) { "Cannot disable shield when not shielded [slot=$slot]" }
        popEffect(HeroEffect.Shield, reason)
    }

    /** Immediately enables an invincible effect. */
    private fun enableInvincible() {
        require(isAlive) { "Cannot be invincible when not alive [slot=$slot]" }
        pushEffect(HeroEffect.Invincible, HeroEffectReason.Null, _config.invincibleDuration)
    }

    /** Immediately disables the current invincible effect. */
    private fun disableInvincible() {
        require(invincible) { "Cannot disable invincible when not invincible [slot=$slot]" }
        popEffect(HeroEffect.Invincible, HeroEffectReason.Null)
    }

    /** Enables imprisoned effect for the specified duration amount. */
    private fun enableImprisoned() {
        require(isAlive) { "Cannot be imprisoned when not alive [slot=$slot]" }
        pushEffect(HeroEffect.Imprisoned, HeroEffectReason.Null, _config.imprisonedDuration)
    }

    /** Immediately disables the current imprisoned effect. */
    private fun disableImprisoned(reason: HeroEffectReason) {
        require(imprisoned) { "Cannot disable imprison when not imprisoned [slot=$slot]" }
        popEffect(HeroEffect.Imprisoned, reason)
    }

    private fun enableSkullEffects() {
        val effects = listOf(
            HeroEffect.SpeedTo1,
            HeroEffect.SpeedTo10,
            HeroEffect.ReverseDirection,
            HeroEffect.PlantBombRepeatedly,
        )
        val randomizer = WeightedRandomizer(effects, listOf(1f, 1f, 1f, 1f))
        val effect = randomizer.random(_random)
        pushEffect(effect, HeroEffectReason.TakeItem, _config.skullEffectDuration)
    }

    private fun disableSkullEffects() {
        listOf(
            HeroEffect.SpeedTo1,
            HeroEffect.SpeedTo10,
            HeroEffect.ReverseDirection,
            HeroEffect.PlantBombRepeatedly,
        ).forEach {
            if (_effectManager.isActive(it)) {
                popEffect(it, HeroEffectReason.TakeItem)
            }
        }
    }

    override fun begin(entityManager: IEntityManager) {
        _entityManager = entityManager
    }

    override fun update(delta: Int) {
        if (!isAlive) {
            // Already dead.
            return
        }
        _effectManager.step(delta) {
            when (it) {
                HeroEffect.Shield -> disableShield(HeroEffectReason.TimeOut)
                HeroEffect.Invincible -> disableInvincible()
                HeroEffect.Imprisoned -> endImprisoned(HeroEffectReason.TimeOut)
                else -> popEffect(it, HeroEffectReason.TimeOut)
            }
        }
        if (_effectManager.isActive(HeroEffect.PlantBombRepeatedly) &&
            _bombManager.getBombs(slot).size < bombCount &&
            _bombManager.getBomb(x.toInt(), y.toInt()) == null) {
            try {
                // FIXME: verify first?
                plantBomb(_timeManager.timestamp.toInt(), false)
            } catch (ex: Exception) {
                // Ignore.
            }
        }
    }

    override fun end() {
        _entityManager = null
    }
}