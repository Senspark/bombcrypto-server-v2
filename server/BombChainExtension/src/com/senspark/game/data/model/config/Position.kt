package com.senspark.game.data.model.config

import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.Serializable

@Serializable
class Position(val x: Int, val y: Int) {
    fun toSfsObject(): ISFSObject {
        return SFSObject().apply {
            putInt("x", x)
            putInt("y", y)
        }
    }
}