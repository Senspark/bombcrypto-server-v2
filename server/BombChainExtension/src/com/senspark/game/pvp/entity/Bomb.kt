package com.senspark.game.pvp.entity

import com.senspark.game.pvp.component.ComponentContainer
import com.senspark.game.pvp.component.IEntityComponent
import com.senspark.game.pvp.component.StateComponent
import com.senspark.game.pvp.manager.IBombManager
import com.senspark.game.pvp.manager.IEntityManager
import com.senspark.game.pvp.manager.ITimeManager
import kotlin.reflect.KClass

class Bomb(
    override val id: Int,
    initialState: IBombState,
    private val _bombManager: IBombManager,
    private val _timeManager: ITimeManager,
) : IBomb {
    private val _componentContainer = ComponentContainer()

    private var _entityManager: IEntityManager? = null
    private var _alive = initialState.isAlive
    private var _slot = initialState.slot
    private var _reason = initialState.reason
    private var _x = initialState.x
    private var _y = initialState.y
    private var _range = initialState.range
    private var _damage = initialState.damage
    private var _piercing = initialState.piercing
    private var _explodeDuration = initialState.explodeDuration
    private var _plantTimestamp = initialState.plantTimestamp

    override val entityManager get() = _entityManager
    override val state
        get() = BombState(
            isAlive = isAlive,
            slot = _slot,
            reason = _reason,
            x = _x,
            y = _y,
            range = _range,
            damage = _damage,
            piercing = _piercing,
            explodeDuration = _explodeDuration,
            explodeRanges = _bombManager.getExplodeRanges(this),
            plantTimestamp = plantTimestamp,
        )
    override val isAlive get() = _alive
    override val slot get() = _slot
    override val reason get() = _reason
    override val x get() = _x
    override val y get() = _y
    override val range get() = _range
    override val damage get() = _damage
    override val piercing get() = _piercing
    override val plantTimestamp get() = _plantTimestamp

    init {
        val stateComponent = StateComponent(this) { state }
        _componentContainer.addComponent(stateComponent)
    }

    override fun <T : IEntityComponent> getComponent(clazz: KClass<T>): T? {
        return _componentContainer.getComponent(clazz)
    }

    override fun applyState(state: IBombState) {
        _alive = state.isAlive
        _slot = state.slot
        _reason = state.reason
        _x = state.x
        _y = state.y
        _range = state.range
        _damage = state.damage
        _piercing = state.piercing
        _explodeDuration = state.explodeDuration
        _plantTimestamp = state.plantTimestamp
    }

    override fun kill() {
        kill(BombReason.Null)
    }

    override fun kill(reason: BombReason) {
        _reason = reason
        _alive = false
        _bombManager.removeBomb(this)
    }

    override fun begin(entityManager: IEntityManager) {
        _entityManager = entityManager
    }

    override fun update(delta: Int) {
        if (!isAlive) {
            return
        }
        val timestamp = _timeManager.timestamp
        if (timestamp + delta >= plantTimestamp + _explodeDuration) {
            // Trigger explode.
            _bombManager.explodeBomb(this)
        }
    }

    override fun end() {
        _entityManager = null
    }
}