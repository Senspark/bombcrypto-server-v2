package com.senspark.game.pvp.manager

import com.senspark.game.pvp.component.IEntityComponent
import com.senspark.game.pvp.entity.IEntity
import kotlin.reflect.KClass

interface IEntityManager {
    val entities: List<IEntity>
    fun <T : IManager> getManager(clazz: KClass<T>): T
    fun addEntity(entity: IEntity)
    fun removeEntity(entity: IEntity)
    fun <T : IEntity> findEntities(clazz: KClass<T>): List<T>
    fun <T : IEntityComponent> findComponents(clazz: KClass<T>): List<T>
    fun processUpdate(delta: Int)
}

inline fun <reified T : IManager> IEntityManager.getManager(): T {
    return getManager(T::class)
}

inline fun <reified T : IEntity> IEntityManager.findEntities(): List<T> {
    return findEntities(T::class)
}

inline fun <reified T : IEntityComponent> IEntityManager.findComponents(): List<T> {
    return findComponents(T::class)
}