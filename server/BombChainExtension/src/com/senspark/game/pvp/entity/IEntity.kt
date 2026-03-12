package com.senspark.game.pvp.entity

import com.senspark.game.pvp.component.IEntityComponent
import com.senspark.game.pvp.manager.IEntityManager
import kotlin.reflect.KClass

interface IEntity {
    val entityManager: IEntityManager?

    /** Whether this entity is alive. */
    val isAlive: Boolean

    fun <T : IEntityComponent> getComponent(clazz: KClass<T>): T?

    /**
     * Kills this entity.
     */
    fun kill()

    fun begin(entityManager: IEntityManager)
    fun update(delta: Int)
    fun end()
}

inline fun <reified T : IEntityComponent> IEntity.getComponent(): T? {
    return getComponent(T::class)
}