package com.senspark.common.cache

import kotlin.time.Duration

class CacheNone : ICacheService {
    override fun test() {
    }

    override fun get(key: String): String = ""

    override fun set(key: String, value: String) {
    }

    override fun set(key: String, values: List<String>) {
    }

    override fun set(key: String, value: String, ttl: Duration) {
    }

    override fun set(key: String, values: List<String>, ttl: Duration) {
    }

    override fun delete(key: String) {
    }

    override fun getFromHash(key: String, field: String): String? {
        return null
    }

    override fun getAllFromHash(key: String): Map<String, String?> {
        return emptyMap()
    }

    override fun setToHash(key: String, field: String, value: String) {
    }

    override fun setToHash(key: String, field: String, value: String, ttl: Duration) {
    }

    override fun deleteFromHash(key: String, field: String) {
    }

    override fun isExistFromSet(key: String, field: String): Boolean {
        return false
    }

    override fun addToSet(key: String, field: String) {
    }

    override fun readSet(key: String): Set<String> {
        return emptySet()
    }

    override fun deleteFromSet(key: String, field: String) {
    }

    override fun destroy() {
    }

    override fun initialize() {
    }
}