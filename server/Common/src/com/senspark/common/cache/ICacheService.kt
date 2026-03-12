package com.senspark.common.cache

import com.senspark.common.service.IGlobalService
import com.senspark.common.service.IService
import kotlin.time.Duration

interface ICacheService : IService, IGlobalService {
    fun test()

    fun get(key: String): String?
    fun set(key: String, value: String)
    fun set(key: String, values: List<String>)
    fun set(key: String, value: String, ttl: Duration)
    fun set(key: String, values: List<String>, ttl: Duration)
    fun delete(key: String)

    fun getFromHash(key: String, field: String): String?
    fun getAllFromHash(key: String): Map<String, String?>
    fun setToHash(key: String, field: String, value: String)
    fun setToHash(key: String, field: String, value: String, ttl: Duration)
    fun deleteFromHash(key: String, field: String)

    fun isExistFromSet(key: String, field: String): Boolean
    fun addToSet(key: String, field: String)
    fun readSet(key: String): Set<String>
    fun deleteFromSet(key: String, field: String)
}