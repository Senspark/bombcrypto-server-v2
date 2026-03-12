package com.senspark.game.utils

import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.EncryptionHelper
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import com.smartfoxserver.v2.extensions.SFSExtension
import javax.crypto.SecretKey

class Sender(
    private val _extension: SFSExtension,
) : ISender {

    override fun initialize() {
    }

    override fun send(cmd: String, params: ISFSObject, user: User, useUdp: Boolean) {
        _extension.send(cmd, params, user, useUdp)
    }

    override fun send(cmd: String, params: ISFSObject, users: List<User>, useUdp: Boolean) {
        _extension.send(cmd, params, users, useUdp)
    }

    override fun sendWithEncrypt(cmd: String, params: ISFSObject, user: User, useUdp: Boolean, aesKey: SecretKey) {
        val json = params.toJson()
        val encryptedData = EncryptionHelper.encryptToBytes(json, aesKey)
        val responseData = SFSObject()
        responseData.putByteArray(SFSField.Data, encryptedData)
        _extension.send(cmd, responseData, user, useUdp)
    }

    override fun sendWithEncrypt(cmd: String, params: ISFSObject, users: Map<User, SecretKey>, useUdp: Boolean) {
        users.forEach {
            sendWithEncrypt(cmd, params, it.key, useUdp, it.value)
        }
    }

}