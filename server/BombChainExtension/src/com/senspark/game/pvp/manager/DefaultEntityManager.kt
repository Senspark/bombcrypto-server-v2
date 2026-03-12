package com.senspark.game.pvp.manager

import com.senspark.game.pvp.activity.IActivity
import com.senspark.game.pvp.component.IEntityComponent
import com.senspark.game.pvp.entity.IEntity
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class DefaultEntityManager(
    private val _maxTimeStep: Int,
    private val _minTimeStep: Int,
) : IEntityManager {
    private val _toBeRemovedEntities = LinkedList<IEntity>()
    private var _entityLocker = 0
    private var _accumulatedTime = 0
    private val _entities = mutableListOf<IEntity>()
    private val _managers = mutableListOf<IManager>()
    private val _activities = mutableListOf<IActivity>()

    override val entities: List<IEntity> get() = _entities

    override fun <T : IManager> getManager(clazz: KClass<T>): T {
        TODO("Not yet implemented")
    }

    override fun addEntity(entity: IEntity) {
        require(entity.isAlive) { "Entity is not alive" }
        addEntityInstantly(entity)
    }

    override fun removeEntity(entity: IEntity) {
        _toBeRemovedEntities.addLast(entity)
        processEntities()
    }

    private fun addEntityInstantly(entity: IEntity) {
        require(entity.entityManager == null)
        _entities.add(entity)
        entity.begin(this)
    }

    private fun removeEntityInstantly(entity: IEntity) {
        entity.end()
        _entities.remove(entity)
        require(entity.entityManager == null)
    }

    private fun processEntities() {
        while (true) {
            if (_toBeRemovedEntities.isEmpty()) {
                return
            }
            if (_entityLocker > 0) {
                return
            }
            val entity = _toBeRemovedEntities.removeFirst()
            ++_entityLocker
            require(!entity.isAlive) { "Entity must not be alive" }
            removeEntityInstantly(entity)
            --_entityLocker
        }
    }

    override fun <T : IEntity> findEntities(clazz: KClass<T>): List<T> {
        return _entities
            .filter { it::class.isSubclassOf(clazz) }
            .map { it as T }
            .toList()
    }

    override fun <T : IEntityComponent> findComponents(clazz: KClass<T>): List<T> {
        return _entities.mapNotNull { it.getComponent(clazz) }
    }

    override fun processUpdate(delta: Int) {
        _accumulatedTime += delta
        while (_accumulatedTime >= _maxTimeStep) {
            _accumulatedTime -= _maxTimeStep
            processUpdateInternal(_maxTimeStep)
        }
        if (_accumulatedTime >= _minTimeStep) {
            processUpdateInternal(_accumulatedTime)
            _accumulatedTime = 0
        }
    }

    private fun processUpdateInternal(delta: Int) {
        for (activity in _activities) {
            ++_entityLocker
            activity.processUpdate(delta)
            --_entityLocker
            processEntities()
        }
    }
}