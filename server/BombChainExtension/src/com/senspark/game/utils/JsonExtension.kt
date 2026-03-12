package com.senspark.game.utils

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlin.collections.Map
import kotlin.reflect.KClass

class JsonExtensionBuilder {
    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

private fun Any.parseArg(): JsonElement {
    return when (this) {
        is Float -> JsonPrimitive(this)
        is Int -> JsonPrimitive(this)
        is List<*> -> JsonArray(this.map { (it ?: throw Exception("Must be not null")).parseArg() })
        is Map<*, *> -> (this as Map<String, Any>).toJObject()
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        else -> throw Exception("Could not find type: ${this::class.simpleName}")
    }
}

fun Map<String, Any>.toJObject(): JsonObject {
    return JsonObject(mapValues {
        it.value.parseArg()
    })
}

inline fun <reified T : Any> T.serialize(): String {
    return JsonExtensionBuilder.json.encodeToString(this)
}

fun Map<String, Any>.serialize(): String {
    return JsonExtensionBuilder.json.encodeToString(toJObject())
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> deserialize(string: String): T {
    return deserialize(string, T::class)
}

@OptIn(InternalSerializationApi::class)
fun <T : Any> deserialize(string: String, clazz: KClass<T>): T {
    val valueSerializer = clazz.serializer()
    return JsonExtensionBuilder.json.decodeFromString(valueSerializer, string)
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> deserializeList(string: String): List<T> {
    val valueSerializer = T::class.serializer()
    val serializer = ListSerializer(valueSerializer)
    return JsonExtensionBuilder.json.decodeFromString(serializer, string)
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> deserializeSet(string: String): Set<T> {
    val valueSerializer = T::class.serializer()
    val serializer = SetSerializer(valueSerializer)
    return JsonExtensionBuilder.json.decodeFromString(serializer, string)
}