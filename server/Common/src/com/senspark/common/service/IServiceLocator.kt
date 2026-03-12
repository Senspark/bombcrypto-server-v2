package com.senspark.common.service

inline fun <reified T : IService> IServiceLocator.resolve(): T {
    val type = T::class.java
    val item = resolve(type)
    if (item is T) {
        return item
    }
    throw Exception("Could not cast to: $type")
}

interface IServiceLocator {
    fun provide(service: IService)
    fun resolve(type: Class<*>): IService
}