package com.senspark.lib.data.manager

import com.senspark.common.utils.ILogger

abstract class BaseDataManager<K, V>(
    protected val logger: ILogger,
) {

    var hashData: MutableMap<K, V> = HashMap()

    open fun initialize(hash: Map<K, V>) {
        hashData = HashMap(hash)
    }

    fun get(key: K): V? {
        if (!hashData.containsKey(key)) {
            logger.error("**********------ Can not find key: $key Class name: ${this::class.java.name}")
        }
        return hashData[key]
    }

    fun put(key: K, value: V) {
        hashData[key] = value
    }

    fun list(): List<V> {
        return ArrayList(hashData.values)
    }

    fun hash(): Map<K, V> {
        return HashMap(hashData)
    }

    fun size(): Int {
        return hashData.size
    }

    fun containsKey(key: K): Boolean {
        return hashData.containsKey(key)
    }
}
