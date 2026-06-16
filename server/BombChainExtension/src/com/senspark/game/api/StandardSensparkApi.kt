package com.senspark.game.api

import com.senspark.common.utils.ILogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody

class ApiException(code: Int, message: String) : Exception(message)

object StandardSensparkApi {
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private suspend fun <T> request(request: Request, serializer: KSerializer<T>, logger: ILogger?): T? = withContext(Dispatchers.IO) {
        HttpClient.getInstance().newCall(request).execute().use {
            val body: ResponseBody? = it.body
            val data = parseBody(body, serializer, logger, debugUrl = request.url.toString())
            if (!it.isSuccessful) {
                if (data != null) {
                    val msg = "Error (code=${it.code}): ${data.error ?: "<no_err_msg>"}"
                    throw ApiException(it.code, msg)
                }
            }
            return@withContext data?.message
        }
    }

    suspend fun <T> get(url: String, serializer: KSerializer<T>, logger: ILogger?): T? {
        val request = Request.Builder()
            .url(url)
            .build()
        return request(request, serializer, logger)
    }

    suspend fun <T> get(url: String, authorization: String, serializer: KSerializer<T>, logger: ILogger?): T? {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", authorization)
            .build()
        return request(request, serializer, logger)
    }

    suspend fun <T> get(
        url: String,
        authorization: String,
        queries: Map<String, String>,
        serializer: KSerializer<T>,
        logger: ILogger?
    ): T? {
        val request = Request.Builder()
            .url(url.plus("?").plus(queries.map { "${it.key}=${it.value}" }.joinToString("&")))
            .addHeader("Authorization", authorization)
            .build()
        return request(request, serializer, logger)
    }

    suspend fun <T> post(
        url: String,
        authorization: String,
        body: JsonObject,
        serializer: KSerializer<T>,
        logger: ILogger?
    ): T? {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $authorization")
            .post(body.toString().toRequestBody(mediaType))
            .build()
        return request(request, serializer, logger)
    }

    private fun <T> parseBody(
        body: ResponseBody?,
        serializer: KSerializer<T>,
        logger: ILogger?,
        debugUrl: String?
    ): GenericBody<T>? {
        if (body == null) {
            return null
        }
        val bodyStr = body.string()
        if (bodyStr.isEmpty()) {
            return null
        }
        return try {
            Json.decodeFromString(GenericBody.serializer(serializer), bodyStr)
        } catch (e: Exception) {
            logger?.error("Failed to parse json body ($debugUrl): $bodyStr")
            e.message?.let { logger?.error(it) }
            null
        }
    }

    @Serializable
    data class GenericBody<T>(
        val success: Boolean,
        val error: String?,
        // Api send error để message là "" thì ko thể parse đc
        // nên ko lấy đc các thông tin error do api gửi về,
        // => fix lại kiểu nullable nếu error api ko gửi message = "" nữa mà gửi null
        val message: T?
    )
}