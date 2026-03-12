package com.senspark.game.api

import com.senspark.game.data.model.nft.HouseDetails
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.declare.EnumConstants
import com.senspark.game.utils.deserializeList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException

class BlockchainHouseDatabase(
    private val _api: IRestApi,
    private val _urlFormat: String,
) : IHouseDatabase {
    @Serializable
    class HouseData(val details: String, val id: Int)
    override fun query(userInfo: IUserInfo, dataType: EnumConstants.DataType): List<HouseDetails> {
        if (userInfo.type == EnumConstants.UserType.FI) {


            val url = String.format(_urlFormat, userInfo.username, dataType.name.lowercase(), userInfo.id)
            val data = _api.get(url)
            val json = Json.parseToJsonElement(data).jsonObject
            val code = json["code"]?.jsonPrimitive?.int ?: throw IOException("Missing code")
            val message = json["message"].toString()
            if (code != 0) {
                throw IOException(message)
            }
            val response = deserializeList<HouseData>(message)
            return response.map {
                HouseDetails(it.details)
            }
        }
        return emptyList()
    }

    override fun queryV3(userInfo: IUserInfo, dataType: EnumConstants.DataType): Boolean {
        return try {
            val url = String.format(_urlFormat, userInfo.username, dataType.name.lowercase(), userInfo.id)
            _api.get(url)
            true
        } catch (e: Exception) {
            false
        }
    }
}
