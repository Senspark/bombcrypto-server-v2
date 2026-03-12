package com.senspark.game.api

import com.senspark.game.api.model.response.DepositResponse
import com.senspark.game.data.model.deposit.UserDeposited
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.utils.deserializeList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException

class BlockchainDepositedDatabase(
    private val _api: IRestApi,
    private val _urlFormat: String
) : IDepositedDatabase {
    override fun query(uid: Int, username: String, dataType: DataType): UserDeposited {
        val url = String.format(_urlFormat, username, dataType.name.lowercase(), uid)
        val data = _api.get(url)
        val json = Json.parseToJsonElement(data).jsonObject
        json["code"]?.jsonPrimitive?.int ?: throw IOException("Missing code")
        val message = deserializeList<DepositResponse>(json["message"].toString())
        var bcoinDeposited = 0f
        var senDeposited = 0f
        message.forEach {
            when (it.type) {
                "BCOIN", "BOMB" -> {
                    bcoinDeposited = it.value
                }

                "SEN" -> {
                    senDeposited = it.value
                }

                else -> throw IOException("Type ${it.type} invalid")
            }
        }
        return UserDeposited(bcoinDeposited, senDeposited)
    }

    override fun queryV3(uid: Int, username: String, dataType: DataType) {
         try {
            val url = String.format(_urlFormat, username, dataType.name.lowercase(), uid)
            _api.get(url)
        } catch (e: Exception) {
            // Do nothing here
        }
    }
}