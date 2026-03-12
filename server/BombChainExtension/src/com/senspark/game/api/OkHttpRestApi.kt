package com.senspark.game.api

import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class UnauthorizedException : Exception("Unauthorized")

class OkHttpRestApi : IRestApi {
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    private fun request(request: Request): String {
        HttpClient.getInstance().newCall(request).execute().use {
            if (!it.isSuccessful) {
                if (it.code == 401) {
                    throw UnauthorizedException()
                }
                throw IOException("Unexpected code: $it")
            }
            val body = it.body ?: throw IOException("Empty body")
            return@request body.string()
        }
    }

    override fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .build()
        return request(request)
    }

    override fun get(url: String, authorization: String): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", authorization)
            .build()
        return request(request)
    }

    override fun get(url: String, authorization: String, queries: Map<String, String>): String {
        val request = Request.Builder()
            .url(url.plus("?").plus(queries.map { "${it.key}=${it.value}" }.joinToString("&")))
            .addHeader("Authorization", authorization)
            .build()
        return request(request)
    }

    override fun post(url: String, authorization: String, body: JsonObject): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $authorization")
            .post(body.toString().toRequestBody(mediaType))
            .build()
        return request(request)
    }

    override fun delete(url: String, authorization: String, body: JsonObject): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $authorization")
            .delete(body.toString().toRequestBody(mediaType))
            .build()
        return request(request)
    }
}