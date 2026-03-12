package com.senspark.game.api

import com.senspark.game.utils.deserialize
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException

class BlockchainInvestedDatabase(
    private val _api: IRestApi,
    private val _urlFormat: String
) : IInvestedDatabase {
    override fun query(username: String): Float {
        @Serializable
        class Data(val value: Float)

        val url = String.format(_urlFormat, username)
        val data = _api.get(url)
        val json = Json.parseToJsonElement(data).jsonObject
        val code = json["code"]?.jsonPrimitive?.int ?: throw IOException("Missing code")
        val message = json["message"].toString()
        if (code != 0) {
            throw IOException(message)
        }
        val response = deserialize<Data>(message)
        return response.value
    }
}