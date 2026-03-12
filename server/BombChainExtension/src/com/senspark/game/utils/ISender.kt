package com.senspark.game.utils

import com.senspark.common.service.IGlobalService
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSObject
import javax.crypto.SecretKey

interface ISender : IGlobalService {
    fun send(cmd: String, params: ISFSObject, user: User, useUdp: Boolean)
    fun send(cmd: String, params: ISFSObject, users: List<User>, useUdp: Boolean)
    fun sendWithEncrypt(cmd: String, params: ISFSObject, user: User, useUdp: Boolean, aesKey: SecretKey)
    fun sendWithEncrypt(cmd: String, params: ISFSObject, users: Map<User, SecretKey>, useUdp: Boolean)
}