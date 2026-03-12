package com.senspark.game.utils

import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.util.MD5
import java.util.*

object SignatureUtils {
    private lateinit var _salt: String
    fun initialize(salt: String) {
        this._salt = salt
    }

    fun verifySignature(username: String, requestId: Int, command: String?, data: ISFSObject): Boolean {
        val timestamp = data.getLong("timestamp")
        val hash = data.getUtfString("hash")
        val text = String.format("%s|%d|%s|%d|%s", username, requestId, command, timestamp, _salt)
        val expectedHash = MD5.getInstance().getHash(text)
        return hash == expectedHash
    }

}