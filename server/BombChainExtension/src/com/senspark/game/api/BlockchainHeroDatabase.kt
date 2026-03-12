package com.senspark.game.api

import com.senspark.common.utils.ILogger
import com.senspark.game.data.model.nft.BlockchainHeroDetails
import com.senspark.game.data.model.nft.IHeroDetails
import com.senspark.game.declare.EnumConstants
import com.senspark.game.utils.deserializeList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.Request
import okhttp3.Response
import okhttp3.OkHttpClient
import java.io.IOException
import java.util.concurrent.TimeUnit

data class BlockchainHeroResponse(
    val details: IHeroDetails,
    val stakeBcoin: Double,
    val stakeSen: Double,
)

class BlockchainHeroDatabase(
    private val _urlFormat: String,
    private val _logger: ILogger,
) : IHeroDatabase {
    @Serializable
    class HeroData(val details: String, val id: Int, val stake_amount: Double, val stake_bcoin: Double, val stake_sen: Double)
    
    companion object{
        private val httpClient = OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build()
    }
    
    override fun query(uid: Int, wallet: String, dataType: EnumConstants.DataType): List<BlockchainHeroResponse> {
        val url = String.format(_urlFormat, wallet, dataType.name.lowercase(), uid)
        val request: Request = Request.Builder().url(url)
            .method("GET", null)
            .build()
        try {
            httpClient.newCall(request).execute().use { response: Response ->
                if (response.isSuccessful && response.body != null) {
                    val jsonBody = response.body!!.string()
                    val json = Json.parseToJsonElement(jsonBody).jsonObject
                    val code = json["code"]?.jsonPrimitive?.int ?: throw IOException("Missing code")
                    val message = json["message"].toString()
                    if (code != 0) {
                        throw IOException(message)
                    }
                    val response = deserializeList<HeroData>(message)
                    return response.map {
                        BlockchainHeroResponse(BlockchainHeroDetails(it.details, dataType), it.stake_bcoin, it.stake_sen)
                    }
                } else {
                    response.close()
                }
            }
        } catch (e: Exception) {
            _logger.error(e)
            return mutableListOf()
        }
        return mutableListOf()
    }

    override fun queryV3(
        uid: Int,
        wallet: String,
        dataType: EnumConstants.DataType
    ): Boolean {
        val url = String.format(_urlFormat, wallet, dataType.name.lowercase(), uid)
        val request: Request = Request.Builder().url(url)
            .method("GET", null)
            .build()
        return try {
            httpClient.newCall(request)
            true
        } catch (e: Exception) {
            _logger.error(e)
            false
        }
    }
}