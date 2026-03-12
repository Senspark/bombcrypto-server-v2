package com.senspark.common.service

import kotlin.reflect.KClass

class ServiceContainer<T : Any>(
    val name: String
) {
    private val _serviceProviders = mutableMapOf<KClass<out T>, () -> T>()
    private val _instances = mutableMapOf<KClass<out T>, T>()

    fun <S : T> register(c: KClass<S>, provider: () -> S) {
        _serviceProviders[c] = provider
    }

    fun <S : T> get(c: KClass<S>): S {
        @Suppress("UNCHECKED_CAST")
        return _instances.getOrPut(c) {
            val provider = _serviceProviders[c]
                ?: throw Exception("No service found for ${c.simpleName}\n${Thread.currentThread().stackTrace.joinToString("\n")}")
            provider()
        } as S
    }

    inline fun <reified S : T> get(): S = get(S::class)

    fun forEach(block: (T) -> Unit) {
        _serviceProviders.keys.forEach { k ->
            get(k).let(block)
        }
    }
}