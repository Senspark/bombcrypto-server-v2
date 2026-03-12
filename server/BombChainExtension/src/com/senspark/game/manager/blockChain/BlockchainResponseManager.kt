package com.senspark.game.manager.blockChain

import com.senspark.common.utils.ILogger
import com.senspark.game.api.BlockchainHeroDatabase.HeroData
import com.senspark.game.api.BlockchainHeroResponse
import com.senspark.game.api.BlockchainHouseDatabase.HouseData
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
import com.senspark.game.manager.IUsersManager
import com.senspark.game.manager.resourceSync.ISyncResourceManager
import com.senspark.game.pvp.HandlerCommand
import com.senspark.game.utils.deserializeList
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.collections.forEach

class BlockchainResponseManager(
    userResourceManger: ISyncResourceManager,
    private val _userDataAccess: IUserDataAccess,
    private val _usersManager: IUsersManager,
    private val _heroBuilder: IHeroBuilder,
    private val _logger: ILogger
) : IBlockchainResponseManager {

    private val _userHouseSync = userResourceManger.houseSyncService
    private val _userHeroSync = userResourceManger.heroSyncService


    private val tagSyncHero = "[${StreamKeys.AP_BL_SYNC_HERO}]"
    private val tagSyncHouse = "[${StreamKeys.AP_BL_SYNC_HOUSE}]"
    private val tagSyncDeposit = "[${StreamKeys.AP_BL_SYNC_DEPOSIT}]"

    override fun listenSyncHero(message: String): Boolean {
        try {
            _logger.log("$tagSyncHero Received sync hero message: $message")
            val blockChainResponse = Json.decodeFromString<BlockChainResponse>(message)
            val data = blockChainResponse.data
            val dataType = EnumConstants.DataType.valueOf(blockChainResponse.type)
            val uid = blockChainResponse.uid
            _logger.log("$tagSyncHero Processing user: $uid, dataType: $dataType")
            val dataList = deserializeList<HeroData>(data)
            val dataSync = dataList.map {
                BlockchainHeroResponse(BlockchainHeroDetails(it.details, dataType), it.stake_bcoin, it.stake_sen)
            }

            val userController = _usersManager.getUserController(uid)

            // Chia làm 2 trường hợp
            // nếu user online thì gọi hàm syncHero của user trong như bình thường
            // Nếu user offline thì sẽ gọi db lấy toàn bộ hero lên để check, thêm sửa xoá và cập lại database theo data từ stream

            //User online, gọi sync hero như bth và notify cho client
            if (userController != null) {
                _logger.log("$tagSyncHero User online, syncing hero and notifying client: $uid")
                val response = userController.masterUserManager.heroFiManager.syncHeroAndGetResponse(dataSync)
                userController.send(HandlerCommand.SyncHeroResponse, response, true)
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

    override fun listenSyncHouse(message: String): Boolean {
        try {
            _logger.log("$tagSyncHouse Received sync house message: $message")
            val blockChainResponse = Json.decodeFromString<BlockChainResponse>(message)
            val data = blockChainResponse.data
            val dataType = EnumConstants.DataType.valueOf(blockChainResponse.type)
            val uid = blockChainResponse.uid
            _logger.log("$tagSyncHouse Processing user: $uid, dataType: $dataType")

            val houseData = deserializeList<HouseData>(data)
            val houseDetailList = houseData.map { HouseDetails(it.details) }
            val userController = _usersManager.getUserController(uid)

            // User online, gọi sync house như bình thường và notify cho client
            if (userController != null) {
                _logger.log("$tagSyncHouse User online, syncing house and notifying client: $uid")
                val data = _userHouseSync.syncHouses(userController, houseDetailList)
                userController.send(HandlerCommand.SyncHouseResponse, data, true)
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
            val blockChainResponse = Json.decodeFromString<BlockChainResponse>(message)
            val data = blockChainResponse.data
            val dataType = EnumConstants.DataType.valueOf(blockChainResponse.type)
            val uid = blockChainResponse.uid
            _logger.log("$tagSyncDeposit Processing user: $uid, dataType: $dataType")

            val depositData = deserializeList<DepositResponse>(data)

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
            val userController = _usersManager.getUserController(uid)
            if (userController != null) {
                userController.masterUserManager.blockRewardManager.loadUserBlockReward()
                val result: ISFSObject = SFSObject()
                result.putSFSArray(SFSField.Rewards, userController.masterUserManager.blockRewardManager.toSfsArrays())
                userController.send(HandlerCommand.SyncDepositResponse, result, true)
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
        val data: String
    )

    data class NewOrUpdatedHero(
        val newHero: List<Hero>,
        val updatedHero: List<Hero>
    )

}