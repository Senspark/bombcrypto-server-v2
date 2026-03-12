package com.senspark.game.utils

import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

fun sfsObjectOf(put: ISFSObject.() -> Unit): ISFSObject {
    val result = SFSObject()
    put(result)
    return result
}

inline fun <reified T> tryGetField(from: ISFSObject, fieldName: String): T? {
    val typeOfT = T::class
    if (!from.containsKey(fieldName)) {
        return null
    }
    return when (typeOfT) {
        Int::class -> from.getInt(fieldName) as T
        String::class -> from.getUtfString(fieldName) as T
        else -> throw Exception("Does not support type $typeOfT to $fieldName")
    }
}