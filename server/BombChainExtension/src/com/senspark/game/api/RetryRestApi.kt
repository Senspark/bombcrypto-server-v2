package com.senspark.game.api

import kotlinx.serialization.json.JsonObject

class RetryRestApi(
    private val _api: IRestApi,
    private val _times: Int
) : IRestApi {
    override fun get(url: String): String {
        var lastEx: Exception? = null
        for (i in 0 until _times) {
            try {
                return _api.get(url)
            } catch (ex: Exception) {
                lastEx = ex
            }
        }
        throw lastEx!!
    }

    override fun get(url: String, authorization: String): String {
        var lastEx: Exception? = null
        for (i in 0 until _times) {
            try {
                return _api.get(url, authorization)
            } catch (ex: Exception) {
                lastEx = ex
            }
        }
        throw lastEx!!
    }

    override fun get(url: String, authorization: String, queries: Map<String, String>): String {
        var lastEx: Exception? = null
        for (i in 0 until _times) {
            try {
                return _api.get(url, authorization, queries)
            } catch (ex: Exception) {
                lastEx = ex
            }
        }
        throw lastEx!!
    }

    override fun post(url: String, authorization: String, body: JsonObject): String {
        var lastEx: Exception? = null
        for (i in 0 until _times) {
            try {
                return _api.post(url, authorization, body)
            } catch (ex: Exception) {
                lastEx = ex
            }
        }
        throw lastEx!!
    }

    override fun delete(url: String, authorization: String, body: JsonObject): String {
        var lastEx: Exception? = null
        for (i in 0 until _times) {
            try {
                return _api.delete(url, authorization, body)
            } catch (ex: Exception) {
                lastEx = ex
            }
        }
        throw lastEx!!
    }
}