package com.senspark.game.api

import kotlinx.serialization.json.JsonObject

interface IRestApi {
    fun get(url: String): String
    fun get(url: String, authorization: String): String
    fun get(url: String, authorization: String, queries: Map<String, String>): String
    fun post(url: String, authorization: String, body: JsonObject): String
    fun delete(url: String, authorization: String, body: JsonObject): String
}