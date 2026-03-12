package com.senspark.game.pvp.handler

import com.senspark.common.pvp.RoomHandler
import com.senspark.common.utils.ILogger
import com.senspark.game.extension.PvpZoneExtension
import com.senspark.game.handler.sol.EncryptionHelper
import com.senspark.game.pvp.entity.UserPvpProperty
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import javax.crypto.SecretKey

abstract class BasePVPEncryptRequestHandler : RoomHandler() {
    protected val logger: ILogger = PvpZoneExtension.AllServices.logger

    override fun handleClientRequest(user: User, params: ISFSObject) {
        handleMsgClientRequest(user, params)
    }

    private fun handleMsgClientRequest(user: User, params: ISFSObject) {
        val aesKey = user.getProperty(UserPvpProperty.AES_KEY) as SecretKey
        val encryptedData = params.getUtfString("data")
        val decryptedData =
            if (encryptedData.isNotEmpty())
                EncryptionHelper.decrypt(encryptedData, aesKey)
            else encryptedData
        val data = SFSObject.newFromJsonData(decryptedData)
        handleGameClientRequest(user, data)
    }

    protected abstract fun handleGameClientRequest(user: User, data: ISFSObject)

    protected fun sendSuccess(
        user: User,
        data: ISFSObject
    ) {
        val aesKey = user.getProperty(UserPvpProperty.AES_KEY) as SecretKey
        val json = data.toJson()
        val encryptedData = EncryptionHelper.encryptToBytes(json, aesKey)
        val responseData = SFSObject()
        responseData.putByteArray("data", encryptedData)
        send(command, responseData, user)
    }
}