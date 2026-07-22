package com.senspark.game.manager.blockChain

import com.senspark.common.utils.ILogger
import com.senspark.game.api.BlockchainHeroDatabase.HeroData
import com.senspark.game.api.BlockchainHeroResponse
import com.senspark.game.api.BlockchainHouseDatabase.HouseData
import com.senspark.game.api.IBlockchainDatabaseManager
import com.senspark.game.api.model.response.DepositResponse
import com.senspark.game.constant.StreamKeys
import com.senspark.game.data.manager.hero.IHeroBuilder
import com.senspark.game.data.model.deposit.UserDeposited
import com.senspark.game.data.model.nft.BlockchainHeroDetails
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.HouseDetails
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.SFSField
import com.senspark.game.extension.coroutines.ICoroutineScope
import com.senspark.game.manager.IUsersManager
import com.senspark.game.manager.resourceSync.ISyncResourceManager
import com.senspark.game.pvp.HandlerCommand
import com.senspark.game.utils.JsonExtensionBuilder
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.forEach

class BlockchainResponseManager(
    userResourceManger: ISyncResourceManager,
    private val _userDataAccess: IUserDataAccess,
    private val _usersManager: IUsersManager,
    private val _heroBuilder: IHeroBuilder,
    private val _databaseManager: IBlockchainDatabaseManager,
    private val _coroutineScope: ICoroutineScope,
    private val _logger: ILogger
) : IBlockchainResponseManager {

    /**
     * Per-user retry counter for forceFresh sync. Increments when we receive a stream message
     * matching DB exactly while caller asked for fresh data — this means the upstream RPC node
     * still returns stale state. We schedule a single retry after [RETRY_DELAY_MS]; if even that
     * still matches DB we give up (the chain may genuinely have no change).
     */
    private val _forceFreshRetries = ConcurrentHashMap<Pair<Int, EnumConstants.DataType>, Int>()
    private val MAX_FORCE_FRESH_RETRIES = 1
    private val RETRY_DELAY_MS = 10_000L

    private val _userHouseSync = userResourceManger.houseSyncService
    private val _userHeroSync = userResourceManger.heroSyncService


    private val tagSyncHero = "[${StreamKeys.AP_BL_SYNC_HERO}]"
    private val tagSyncHouse = "[${StreamKeys.AP_BL_SYNC_HOUSE}]"
    private val tagSyncDeposit = "[${StreamKeys.AP_BL_SYNC_DEPOSIT}]"

    override fun listenSyncHero(message: String): Boolean {
        try {
            _logger.log("$tagSyncHero Received sync hero message: $message")
            val blockChainResponse = JsonExtensionBuilder.json.decodeFromString<BlockChainResponse>(message)
            val data = blockChainResponse.data
            val dataType = EnumConstants.DataType.valueOf(blockChainResponse.type)
            val uid = blockChainResponse.uid
            val forceFresh = blockChainResponse.forceFresh
            _logger.log("$tagSyncHero Processing user: $uid, dataType: $dataType, forceFresh: $forceFresh")
            val dataList = JsonExtensionBuilder.json.decodeFromJsonElement<List<HeroData>>(data)
            val dataSync = dataList.map {
                BlockchainHeroResponse(BlockchainHeroDetails(it.details, dataType), it.stake_bcoin, it.stake_sen)
            }

            val userController = _usersManager.getUserController(uid, dataType, EnumConstants.Landing.TREASURE)

            // Chia làm 2 trường hợp
            // nếu user online thì gọi hàm syncHero của user trong như bình thường
            // Nếu user offline thì sẽ gọi db lấy toàn bộ hero lên để check, thêm sửa xoá và cập lại database theo data từ stream

            //User online, gọi sync hero như bth và notify cho client
            if (userController != null) {
                if (forceFresh && isIdenticalToDb(uid, dataType, dataSync)) {
                    val key = Pair(uid, dataType)
                    val attempts = _forceFreshRetries.getOrDefault(key, 0)
                    if (attempts < MAX_FORCE_FRESH_RETRIES) {
                        _forceFreshRetries[key] = attempts + 1
                        val wallet = userController.walletAddress
                        if (wallet.isNullOrEmpty()) {
                            _logger.log("$tagSyncHero forceFresh data identical to DB but wallet missing, skip retry: uid=$uid")
                            _forceFreshRetries.remove(key)
                            return true
                        }
                        _logger.log("$tagSyncHero forceFresh data identical to DB, scheduling retry attempt=${attempts + 1} after ${RETRY_DELAY_MS}ms: uid=$uid")
                        _coroutineScope.scope.launch(Dispatchers.IO) {
                            delay(RETRY_DELAY_MS)
                            _databaseManager.heroDatabase.queryV4(uid, wallet, dataType, true)
                        }
                    } else {
                        _logger.log("$tagSyncHero forceFresh data still identical after retry, giving up: uid=$uid")
                        _forceFreshRetries.remove(key)
                    }
                    // Don't push: client trusts a forceFresh push to mean "data really changed"
                    return true
                }
                _forceFreshRetries.remove(Pair(uid, dataType))
                _logger.log("$tagSyncHero User online, syncing hero and notifying client: $uid")
                val response = userController.masterUserManager.heroFiManager.syncHeroAndGetResponse(dataSync)
                userController.sendDataEncryption(HandlerCommand.SyncHeroResponse, response, true)
                return true
            }

            // User offline, cập nhật db
            else {
                _logger.log("$tagSyncHero User is offline, only update database: $uid")
                _userHeroSync.syncHeroOffline(uid, dataType, dataSync)
                return false
            }
        } catch (e: Exception) {
            _logger.error("$tagSyncHero Error in listenSyncHero: ${e.message}", e)
            return false
        }
    }

    /**
     * Returns true if the incoming on-chain hero list matches the current DB FI hero set exactly:
     * same hero IDs, same details hex, same stake amounts. Used to detect stale RPC responses
     * after a forceFresh sync — when caller expected change but chain hasn't propagated yet.
     */
    private fun isIdenticalToDb(
        uid: Int,
        dataType: EnumConstants.DataType,
        incoming: List<BlockchainHeroResponse>,
    ): Boolean {
        val dbHeroes = _heroBuilder.getFiHeroes(uid, dataType, Int.MAX_VALUE, 0).values
            .filter { it.type == EnumConstants.HeroType.FI }
        if (incoming.size != dbHeroes.size) return false
        val incomingMap = incoming.associateBy { it.details.heroId }
        val dbMap = dbHeroes.associateBy { it.heroId }
        if (incomingMap.keys != dbMap.keys) return false
        for ((id, inc) in incomingMap) {
            val db = dbMap[id] ?: return false
            if (inc.details.details != db.details.details) return false
            if (inc.stakeBcoin != db.stakeBcoin) return false
            if (inc.stakeSen != db.stakeSen) return false
        }
        return true
    }

    override fun listenSyncHouse(message: String): Boolean {
        try {
            _logger.log("$tagSyncHouse Received sync house message: $message")
            val blockChainResponse = JsonExtensionBuilder.json.decodeFromString<BlockChainResponse>(message)
            val data = blockChainResponse.data
            val dataType = EnumConstants.DataType.valueOf(blockChainResponse.type)
            val uid = blockChainResponse.uid
            _logger.log("$tagSyncHouse Processing user: $uid, dataType: $dataType")

            val houseData = JsonExtensionBuilder.json.decodeFromJsonElement<List<HouseData>>(data)
            val houseDetailList = houseData.map { HouseDetails(it.details) }
            val userController = _usersManager.getUserController(uid, dataType, EnumConstants.Landing.TREASURE)

            // User online, gọi sync house như bình thường và notify cho client
            if (userController != null) {
                _logger.log("$tagSyncHouse User online, syncing house and notifying client: $uid")
                val data = _userHouseSync.syncHouses(userController, houseDetailList)
                userController.sendDataEncryption(HandlerCommand.SyncHouseResponse, data, true)
                return true
            }
            // User offline, cập nhật db
            else {
                _logger.log("$tagSyncHouse User is offline, only update database: $uid")
                _userHouseSync.syncHousesOffline(uid, dataType, houseDetailList, _heroBuilder)
                return false
            }
        } catch (e: Exception) {
            _logger.error("$tagSyncHouse Error when process: $e")
            return false
        }
    }

    override fun listenSyncDeposit(message: String): Boolean {
        try {
            _logger.log("$tagSyncDeposit Received sync deposit message: $message")
            val blockChainResponse = JsonExtensionBuilder.json.decodeFromString<BlockChainResponse>(message)
            val data = blockChainResponse.data
            val dataType = EnumConstants.DataType.valueOf(blockChainResponse.type)
            val uid = blockChainResponse.uid
            _logger.log("$tagSyncDeposit Processing user: $uid, dataType: $dataType")

            val depositData = JsonExtensionBuilder.json.decodeFromJsonElement<List<DepositResponse>>(data)

            var bcoinDeposited = 0f
            var senDeposited = 0f
            depositData.forEach {
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
            val userDeposited = UserDeposited(bcoinDeposited, senDeposited)
            _userDataAccess.syncDeposit(uid, dataType, userDeposited)

            //Nếu user online thì update block reward và notify cho client
            val userController = _usersManager.getUserController(uid, dataType, EnumConstants.Landing.TREASURE)
            if (userController != null) {
                userController.masterUserManager.blockRewardManager.loadUserBlockReward()
                val result: ISFSObject = SFSObject()
                result.putSFSArray(SFSField.Rewards, userController.masterUserManager.blockRewardManager.toSfsArrays())
                userController.sendDataEncryption(HandlerCommand.SyncDepositResponse, result, true)
            }
            return true
        } catch (e: Exception) {
            _logger.error("$tagSyncDeposit Error when process: $e")
            return false
        }
    }

    override fun initialize() {
    }

    @Serializable
    data class BlockChainResponse(
        val uid: Int,
        val type: String,
        val data: JsonElement,
        val forceFresh: Boolean = false,
    )

    data class NewOrUpdatedHero(
        val newHero: List<Hero>,
        val updatedHero: List<Hero>
    )

}