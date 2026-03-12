package com.senspark.common.utils

import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray

fun sfsArrayOf(vararg elements: Any): ISFSArray {
    val result = SFSArray()
    elements.forEach { result.add(it) }
    return result
}

fun <T> ISFSArray.map(transform: (ISFSObject) -> T): List<T> {
    val result = mutableListOf<T>()
    for (i in 0 until size()) {
        result.add(transform(getSFSObject(i)))
    }
    return result
}

fun <K, V> ISFSArray.associateBy(
    selector: (ISFSObject) -> K,
    transform: (ISFSObject) -> V
): Map<K, V> {
    val result = mutableMapOf<K, V>()
    for (i in 0 until size()) {
        val obj = getSFSObject(i)
        result[selector(obj)] = transform(obj)
    }
    return result
}

fun <P, R> Collection<P>.toSFSArray(transform: (P) -> R): ISFSArray {
    val result = SFSArray()
    forEach { result.add(transform(it)) }
    return result
}

fun <K, V, R> Map<K, V>.toSFSArray(transform: (Map.Entry<K, V>) -> R): ISFSArray {
    return entries.toSFSArray { transform(it) }
}

fun <T> ISFSArray.add(value: T) {
    when (value) {
        is ISFSObject -> addSFSObject(value)
        is ISFSArray -> addSFSArray(value)
        is Int -> addInt(value)
        else -> throw Exception("Could not add $value")
    }
}

fun <T> ISFSArray.toList(transform: (ISFSObject) -> T): List<T> {
    val result = mutableListOf<T>()
    for (i in 0 until size()) {
        val obj = getSFSObject(i)
        result.add(transform(obj))
    }
    return result
}
