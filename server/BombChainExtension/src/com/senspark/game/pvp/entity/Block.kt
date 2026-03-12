package com.senspark.game.pvp.entity

import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.component.ComponentContainer
import com.senspark.game.pvp.component.IEntityComponent
import com.senspark.game.pvp.component.StateComponent
import com.senspark.game.pvp.manager.IEntityManager
import com.senspark.game.pvp.manager.IMapManager
import kotlin.math.max
import kotlin.reflect.KClass

class Block(
    override val x: Int,
    override val y: Int,
    private val _initialState: IBlockState,
    private val _logger: ILogger,
    private val _mapManager: IMapManager,
) : IBlock {
    companion object {
        fun createHardBlock(
            x: Int,
            y: Int,
            reason: BlockReason,
            logger: ILogger,
            mapManager: IMapManager,
        ): IBlock {
            return Block(
                x = x,
                y = y,
                _initialState = BlockState(
                    isAlive = true,
                    reason = reason,
                    type = BlockType.Hard,
                    health = 1,
                    maxHealth = 1,
                ),
                _logger = logger,
                _mapManager = mapManager,
            )
        }
    }

    private val _componentContainer = ComponentContainer()

    private var _entityManager: IEntityManager? = null
    private var _reason = _initialState.reason
    private var _type = _initialState.type
    private var _health = _initialState.health
    private var _maxHealth = _initialState.maxHealth

    override val entityManager get() = _entityManager
    override val state
        get() = BlockState(
            isAlive = isAlive,
            reason = _reason,
            type = type,
            health = _health,
            maxHealth = _maxHealth,
        )
    override val isAlive get() = _health > 0
    override val reason get() = _reason
    override val type get() = _type

    init {
        val stateComponent = StateComponent(this) { state }
        _componentContainer.addComponent(stateComponent)
    }

    override fun <T : IEntityComponent> getComponent(clazz: KClass<T>): T? {
        return _componentContainer.getComponent(clazz)
    }

    override fun applyState(state: IBlockState) {
        _reason = state.reason
        _type = state.type
        _health = state.health
        _maxHealth = state.maxHealth
    }

    override fun kill() {
        kill(BlockReason.Null)
    }

    override fun kill(reason: BlockReason) {
        _reason = reason
        _health = 0
        _mapManager.removeBlock(this)
    }

    override fun takeDamage(amount: Int) {
        if (type == BlockType.Hard) {
            return
        }
        _health = max(0, _health - amount)
        _logger.log("[Block:damage] amount=$amount health=$_health x=$x y=$y")
    }

    override fun begin(entityManager: IEntityManager) {
        _entityManager = entityManager
    }

    override fun update(delta: Int) {
    }

    override fun end() {
        _entityManager = null
    }
}