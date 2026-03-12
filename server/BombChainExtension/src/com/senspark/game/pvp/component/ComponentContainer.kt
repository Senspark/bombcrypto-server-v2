package com.senspark.game.pvp.component

import kotlin.reflect.KClass

class ComponentContainer {
    private val _components = mutableMapOf<KClass<*>, IEntityComponent>()

    fun addComponent(component: IEntityComponent) {
        _components[component::class] = component
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : IEntityComponent> getComponent(clazz: KClass<T>): T? {
        return _components[clazz] as T?
    }
}

inline fun <reified T : IEntityComponent> ComponentContainer.getComponent(): T? {
    return getComponent(T::class)
}