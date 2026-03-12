package com.senspark.game.manager.convertToken

import com.senspark.common.cache.ICacheService
import com.senspark.common.service.IScheduler
import com.senspark.common.utils.ILogger
import com.senspark.game.api.IRestApi
import com.senspark.game.api.OkHttpRestApi
import com.senspark.game.constant.CachedKeys
import com.senspark.game.controller.IUserController
import com.senspark.game.data.RewardData
import com.senspark.game.data.model.config.SwapTokenRealtimeConfig
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.customEnum.ChangeRewardReason
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.IEnvManager
import com.senspark.game.service.IPvpDataAccess
import com.senspark.game.utils.SmartFoxScheduler
import com.senspark.game.utils.deserialize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Serializable
data class PriceTokenResponseData(
    @SerialName("bnb_sen")
    val priceSenBNB: Float,
    @SerialName("polygon_sen")
    val priceSenPolygon: Float,
    @SerialName("bnb_bcoin")
    val priceBcoinBNB: Float,
    @SerialName("polygon_bcoin")
    val priceBcoinPolygon: Float,
)

class SwapTokenRealtimeManager(
    private val _envManager: IEnvManager,
    private val _gameDataAccess: IGameDataAccess,
    private val _shopDataAccess: IShopDataAccess,
    private val _databaseManager: IPvpDataAccess,
    private val _redis: ICacheService,
    private val _logger: ILogger
) : ISwapTokenRealtimeManager {

    private val _api: IRestApi = OkHttpRestApi()
    private val tokenPrice: MutableMap<DataType, Map<BLOCK_REWARD_TYPE, Float>> = mutableMapOf()
    private lateinit var swapRealtimeConfig: SwapTokenRealtimeConfig
    private var remainingTotalSwap: Float = 0f

    private val userSwapMap = ConcurrentHashMap<Int, Long>()
    private val _scheduler: IScheduler = SmartFoxScheduler(1, _logger)

    override fun initialize() {
        tokenPrice.putAll(reloadTokenPrice())
        swapRealtimeConfig = _shopDataAccess.loadSwapTokenRealtimeConfig()
        remainingTotalSwap = swapRealtimeConfig.remainingTotalSwap

        _scheduler.schedule(
            "UserSwapGem",
            0,
            60 * 1000,
            ::cleanUpUserSwapMap,
        )
    }

    private fun cleanUpUserSwapMap() {
        val oneMinuteAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1)
        userSwapMap.entries.removeIf { it.value < oneMinuteAgo }
    }

    /**
     *  return true if user is in cheat list
     */
    private fun isInBlackList(userId: Int): Boolean {
        return _redis.isExistFromSet(CachedKeys.BLACK_LIST_SWAP_GEM, userId.toString())
    }
    private fun isInWhiteListUser(userId: Int): Boolean {
        return _redis.isExistFromSet(CachedKeys.WHITE_LIST_SWAP_GEM, userId.toString())
    }

    override fun destroy() {
    }

    override fun reloadTokenPrice(): Map<DataType, Map<BLOCK_REWARD_TYPE, Float>> {
        try {
            val url = String.format(_envManager.getPriceTokenUrl)
            val bodyJson = _api.get(url, _envManager.apSignatureToken)
            val jsonData = Json.parseToJsonElement(bodyJson).jsonObject

            val code = jsonData["code"]?.jsonPrimitive?.int ?: throw CustomException("Invalid response: code not found")
            if (code != 0) {
                throw CustomException("Time out or error")
            }
            val message = jsonData["message"] ?: throw CustomException("Invalid response: message not found")
            val priceTokens = deserialize<PriceTokenResponseData>(message.toString())

            val result: MutableMap<DataType, Map<BLOCK_REWARD_TYPE, Float>> = mutableMapOf()
            result[DataType.BSC] = mutableMapOf(
                BLOCK_REWARD_TYPE.BCOIN to priceTokens.priceBcoinBNB,
                BLOCK_REWARD_TYPE.SENSPARK to priceTokens.priceSenBNB
            )
            result[DataType.POLYGON] = mutableMapOf(
                BLOCK_REWARD_TYPE.BCOIN to priceTokens.priceBcoinPolygon,
                BLOCK_REWARD_TYPE.SENSPARK to priceTokens.priceSenPolygon
            )
            tokenPrice.clear()
            tokenPrice.putAll(result)
            return result
        } catch (ex: Exception) {
            _logger.error("API Fail reload token price:\n" + ex.message)
            return mutableMapOf()
        }
    }

    override fun previewConversion(
        balance: Float,
        networkType: ISwapTokenRealtimeManager.NetworkType,
        tokenType: Int
    ): Float {
        val network = convert(networkType)
        val token = BLOCK_REWARD_TYPE.valueOf(tokenType)
        return balance * getGemRatio(token, network)
    }

    override fun tokenConvert(
        userId: Int,
        userController: IUserController,
        balance: Float,
        networkType: ISwapTokenRealtimeManager.NetworkType,
        tokenType: Int
    ): Float {
        //First check all user from cheat list, those user will be blocked
        if (!isInWhiteListUser(userId)) {
            this._logger.log("User not in whiteList not allow swap gem - uid: ${userController.userId}")
            throw CustomException("You are not allowed to swap gem")
        }
        _logger.log("Whitelist user swap gem - uid: ${userController.userId}")

        // Vẫn giữ logic cũ của black list
//        if (isInBlackList(userController.userId)) {
//            this._logger.log("User in Blacklist try to swap gem - uid: ${userController.userId}")
//            throw CustomException("You are not allowed to swap gem")
//        }
        if (balance <= 0) {
            throw CustomException("Invalid value: $balance")
        }
        if (!userController.checkHash()) {
            throw Exception("Invalid hash")
        }
        if (balance < swapRealtimeConfig.minGemSwap) {
            throw CustomException("Swap minimum ${swapRealtimeConfig.minGemSwap} Gems\n Please try again")
        }

        val timeSwapEachDay = swapRealtimeConfig.timesSwapEachDay
        // Đảm bảo user ko bấm qúa nhanh bypass db check
        if (userSwapMap.containsKey(userId)) {
            throw CustomException("You only can swap $timeSwapEachDay time per day")
        }

        // Add user to the map with the current timestamp
        userSwapMap[userId] = System.currentTimeMillis()

        if (!_gameDataAccess.checkValidSwapGem(userId, timeSwapEachDay)) {
            throw CustomException("You only can swap $timeSwapEachDay time per day")
        }

        val amountDollar = balance * swapRealtimeConfig.priceGem
        if (amountDollar > swapRealtimeConfig.maxAmountEachTime) {
            throw CustomException("Swap limit reached for the day \n Please try again tomorrow")
        }
        if (amountDollar > remainingTotalSwap) {
            throw CustomException("Swap Pool will be refill \n at approximately 00h00 UTC")
        }
        val userBlockRewardManager = userController.masterUserManager.blockRewardManager
        userBlockRewardManager.loadUserBlockReward()
        val gem = userBlockRewardManager.getRewardValue(BLOCK_REWARD_TYPE.GEM, DataType.TR)
        if (gem < balance) {
            throw CustomException("Swap amount exceeds your balance \n Please try again")
        }


        val previewBalance = previewConversion(balance, networkType, tokenType)
        val token = BLOCK_REWARD_TYPE.valueOf(tokenType)
        val network = convert(networkType)
        val rewards = listOf(
            RewardData(
                BLOCK_REWARD_TYPE.GEM.value,
                BLOCK_REWARD_TYPE.GEM.name,
                DataType.TR.name,
                -balance,
            ),
            RewardData(
                token.value,
                token.name,
                network.name,
                previewBalance,
            )
        )
        _databaseManager.updateUserReward(userId, rewards, ChangeRewardReason.SWAP_TOKEN)
        remainingTotalSwap -= amountDollar
        _gameDataAccess.updateRemainingTotalSwap(remainingTotalSwap)

        val unitPrice = tokenPrice[network]!![token]!!
        _gameDataAccess.logSwapGem(userId, token, previewBalance, unitPrice, network)
        return previewBalance
    }

    override fun refillRemainingTotalSwap() {
        remainingTotalSwap = swapRealtimeConfig.totalSwapEachDay.toFloat()
        _gameDataAccess.updateRemainingTotalSwap(remainingTotalSwap)
    }

    override fun getTimeUpdatePrice(): Int {
        return swapRealtimeConfig.timeUpdatePrice
    }

    override fun getMinGemSwap(): Int {
        return swapRealtimeConfig.minGemSwap
    }

    private fun getGemRatio(tokenType: BLOCK_REWARD_TYPE, networkType: DataType): Float {
        return swapRealtimeConfig.priceGem / tokenPrice[networkType]!![tokenType]!!
    }

    private fun convert(d: ISwapTokenRealtimeManager.NetworkType): DataType {
        return when (d) {
            ISwapTokenRealtimeManager.NetworkType.BNB -> DataType.BSC
            ISwapTokenRealtimeManager.NetworkType.POLYGON -> DataType.POLYGON
            else -> throw Exception("Does not support this type: ${d.name}")
        }
    }
}