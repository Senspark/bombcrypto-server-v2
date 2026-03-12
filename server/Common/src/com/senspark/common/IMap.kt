package com.senspark.common

import kotlin.reflect.KClass

inline fun <reified T> IMap.getValue(): T {
    val type = T::class
    val item = values[type]
    if (item is T) {
        return item
    }
    throw Exception("Could not find handler: $type")
}

interface IMap {
    val values: Map<KClass<*>, Any>
}