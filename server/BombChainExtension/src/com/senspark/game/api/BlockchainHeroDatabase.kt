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
    private val _api: IRestApi,
    private val _urlFormat: String,
    private val _urlFormatV4: String,
    private val _refreshStakeUrl: String,
    private val _logger: ILogger,
) : IHeroDatabase {
    @Serializable
    class HeroData(val details: String, val id: Int, val stake_bcoin: Double, val stake_sen: Double)
    
    override fun query(uid: Int, wallet: String, dataType: EnumConstants.DataType): List<BlockchainHeroResponse> {
        try {
            val url = String.format(_urlFormat, wallet, dataType.name.lowercase(), uid)
            val jsonBody = _api.get(url)
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
        } catch (e: Exception) {
            _logger.error(e)
            return mutableListOf()
        }
    }

    override fun queryV3(
        uid: Int,
        wallet: String,
        dataType: EnumConstants.DataType
    ): Boolean {
        return try {
            val url = String.format(_urlFormat, wallet, dataType.name.lowercase(), uid)
            _api.get(url)
            true
        } catch (e: Exception) {
            _logger.error(e)
            false
        }
    }

    override fun queryV4(
        uid: Int,
        wallet: String,
        dataType: EnumConstants.DataType,
        forceFresh: Boolean,
    ): Boolean {
        return try {
            val url = String.format(_urlFormatV4, wallet, dataType.name.lowercase(), uid, if (forceFresh) "1" else "0")
            _api.get(url)
            true
        } catch (e: Exception) {
            _logger.error(e)
            false
        }
    }

    override fun refreshHeroStake(
        uid: Int,
        heroId: Int,
        dataType: EnumConstants.DataType,
        txHash: String,
    ): Boolean {
        // ap-blockchain keys its config map by chain key (BSC / POL), not DataType.name.
        // Map here so the upstream lookup matches.
        val network = when (dataType) {
            EnumConstants.DataType.BSC -> "BSC"
            EnumConstants.DataType.POLYGON -> "POL"
            else -> return false
        }
        return try {
            val body = buildJsonObject {
                put("uid", uid)
                put("heroId", heroId)
                put("network", network)
                put("txHash", txHash)
            }
            _api.post(_refreshStakeUrl, body)
            true
        } catch (e: Exception) {
            _logger.error(e)
            false
        }
    }
}