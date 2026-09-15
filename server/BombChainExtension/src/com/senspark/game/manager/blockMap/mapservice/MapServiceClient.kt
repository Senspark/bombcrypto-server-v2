package com.senspark.game.manager.blockMap.mapservice

import com.senspark.game.utils.deserialize
import com.senspark.game.utils.serialize
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

// Separate from HttpClient: MapService is in-memory only, so timeouts are much tighter.
class MapServiceClient(private val baseUrl: String) : IMapServiceClient {
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(300, TimeUnit.MILLISECONDS)
        .readTimeout(500, TimeUnit.MILLISECONDS)
        .writeTimeout(500, TimeUnit.MILLISECONDS)
        .build()

    private fun url(path: String) = "$baseUrl$path"

    private fun execute(request: Request, allow404: Boolean = false, allow409: Boolean = false): String? {
        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw MapServiceException("MapService call failed: ${e.message}", e)
        }
        response.use {
            if (it.code == 404) {
                if (allow404) return null
                throw MapServiceSessionNotFoundException()
            }
            if (it.code == 409) {
                if (allow409) return null
                throw MapServiceException("MapService HTTP 409 for ${request.url}")
            }
            if (!it.isSuccessful) {
                throw MapServiceException("MapService HTTP ${it.code} for ${request.url}")
            }
            return it.body?.string() ?: ""
        }
    }

    private fun post(path: String, jsonBody: String, allow404: Boolean = false, allow409: Boolean = false): String? {
        val request = Request.Builder()
            .url(url(path))
            .post(jsonBody.toRequestBody(mediaType))
            .build()
        return execute(request, allow404, allow409)
    }

    override fun initSession(sessionKey: String, request: MsMapInitRequest) {
        post("/sessions/$sessionKey/init", request.serialize(), allow409 = true)
    }

    override fun replaceMap(sessionKey: String, request: MsMapReplaceRequest) {
        post("/sessions/$sessionKey/map", request.serialize())
    }

    override fun deleteSession(sessionKey: String) {
        val httpRequest = Request.Builder()
            .url(url("/sessions/$sessionKey"))
            .delete()
            .build()
        execute(httpRequest, allow404 = true)
    }

    override fun getTargets(sessionKey: String, request: MsTargetsRequest): MsTargetsResponse {
        val body = post("/sessions/$sessionKey/targets", request.serialize())
            ?: throw MapServiceException("Empty response from MapService /targets")
        return deserialize(body)
    }

    override fun plantBomb(sessionKey: String, request: MsPlantRequest): MsPlantResponse {
        val body = post("/sessions/$sessionKey/plant", request.serialize())
            ?: throw MapServiceException("Empty response from MapService /plant")
        return deserialize(body)
    }

    override fun explode(sessionKey: String, request: MsExplodeRequest): MsExplodeResponse {
        val body = post("/sessions/$sessionKey/explode", request.serialize())
            ?: throw MapServiceException("Empty response from MapService /explode")
        return deserialize(body)
    }
}
