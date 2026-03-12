package com.senspark.common.cache

class MemoryCache<T>(
    cacheDurationSeconds: Float,
    private val fetchData: suspend () -> T
) {
    private var cache: T? = null
    private var lastFetchTime: Long = 0
    private val cacheDurationMs: Long = (cacheDurationSeconds * 1000).toLong()

    suspend fun getData(): T {
        val currentTime = System.currentTimeMillis()
        if (cache == null || (currentTime - lastFetchTime) >= cacheDurationMs) {
            cache = fetchData()
            lastFetchTime = currentTime
        }
        return cache!!
    }
}