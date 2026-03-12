package com.senspark.game.manager.rock

import com.senspark.game.api.IRestApi
import com.senspark.game.api.OkHttpRestApi
import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.hero.IHeroBuilder
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.db.IRewardDataAccess
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.EnumConstants.HeroType
import com.senspark.game.declare.customEnum.ChangeRewardReason
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.hero.IUserHeroFiManager
import com.senspark.game.service.IHeroUpgradeShieldManager
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap

enum class Status {
    PENDING,
    FALSE,
    ERROR,
    DONE
}

data class RockAmount(val heroS: Float, val heroL: Float)

class UserRockManager(
    private val _shopDataAccess: IShopDataAccess,
    private val _rewardDataAccess: IRewardDataAccess,
    private val _gameDataAccessPostgreSql: IGameDataAccess,
    private val _envManager: IEnvManager,
    private val _heroUpgradeShieldManager: IHeroUpgradeShieldManager,
    private val _heroBuilder: IHeroBuilder,
) : IUserRockManager {

    override lateinit var convertHeroRockConfig: Map<Int, RockAmount>
    private val _pendingTransactions: ConcurrentHashMap<String, Int> = ConcurrentHashMap(mapOf())
    private val _api: IRestApi = OkHttpRestApi()

    override fun initialize() {
        convertHeroRockConfig = _shopDataAccess.loadBurnHeroConfig()
    }

    override fun setConfig(convertHeroRockConfig: Map<Int, RockAmount>) {
        this.convertHeroRockConfig = convertHeroRockConfig
    }

    override fun upgradeShieldLevel(userController: IUserController, hero: Hero): Pair<Int, String> {
        val levelShieldUpgrade = hero.shield.level + 1
        if (levelShieldUpgrade > 3) {
            throw CustomException("Hero max upgrade")
        }
        val userBlockRewardManager = userController.masterUserManager.blockRewardManager
        val price = _heroUpgradeShieldManager.getPrice(levelShieldUpgrade, hero.rarity)
        userBlockRewardManager.checkEnoughReward(price, BLOCK_REWARD_TYPE.ROCK)

        _rewardDataAccess.subUserReward(
            userController.userId,
            BLOCK_REWARD_TYPE.ROCK,
            price,
            DataType.TR,
            ChangeRewardReason.UPGRADE_SHIELD
        )
        userBlockRewardManager.loadUserBlockReward()

        val result = getCommandUpgradeShield(userController.walletAddress, hero.heroId, userController.dataType.name)
        return result
    }

    override fun createRock(
        userController: IUserController,
        tx: String,
        walletAddress: String,
        listIdHero: List<Int>
    ): Float {
        val uid = userController.userId
        val network = userController.userInfo.dataType

        if (_pendingTransactions.containsKey(tx)) {
            throw CustomException("Duplicate request")
        }
        if (!checkDatabaseGuard(uid, tx, network)) {
            throw CustomException("Duplicate database")
        }
        _pendingTransactions[tx] = uid

        val userHeroFiManager = userController.masterUserManager.heroFiManager
        val userBlockRewardManager = userController.masterUserManager.blockRewardManager
        var totalRockReceive = sumRockReceive(userHeroFiManager, listIdHero)
        addTransactionToDatabase(uid, tx, listIdHero, totalRockReceive, network)
        userHeroFiManager.syncBomberMan()

        try {
            if (checkValidTransaction(tx, listIdHero, walletAddress, network.name)) {
                updateStatus(uid, tx, network, Status.DONE)
                if (totalRockReceive == 0f) {
                    val listHero = _heroBuilder.getHeroFiFromDatabase(network, listIdHero, HeroType.FI.value)
                    val listRockReceive = listHero.map {
                        if (it.isHeroS)
                            convertHeroRockConfig[it.rarity]!!.heroS
                        else convertHeroRockConfig[it.rarity]!!.heroL
                    }
                    totalRockReceive = listRockReceive.sum()
                }
                _rewardDataAccess.addUserBlockReward(
                    uid,
                    BLOCK_REWARD_TYPE.ROCK,
                    DataType.TR,
                    totalRockReceive,
                    0f,
                    ChangeRewardReason.CREATE_ROCK
                )
                userBlockRewardManager.loadUserBlockReward()
                return userBlockRewardManager.getTotalRockHaving()
            } else {
                updateStatus(uid, tx, network, Status.FALSE)
                return 0f
            }
        } catch (e: Exception) {
            updateStatus(uid, tx, network, Status.ERROR)
            throw CustomException(e.message!!)
        } finally {
            _pendingTransactions.remove(tx)
        }
    }

    private fun sumRockReceive(userHeroFiManager: IUserHeroFiManager, listHeroId: List<Int>): Float {
        var result = 0f
        listHeroId.forEach {
            val hero = userHeroFiManager.getHero(it, HeroType.FI)
            if (hero != null) {
                if (hero.isHeroS) {
                    result += convertHeroRockConfig[hero.rarity]?.heroS ?: 0f
                } else {
                    result += convertHeroRockConfig[hero.rarity]?.heroL ?: 0f
                }
            }
        }
        return result
    }

    private fun checkValidTransaction(
        tx: String,
        listIdHero: List<Int>,
        walletAddress: String,
        network: String
    ): Boolean {
        val body = Json.run {
            buildJsonObject {
                put("tx", tx)
                put("wallet_address", walletAddress)
                put("hero_ids", JsonArray(listIdHero.map { JsonPrimitive(it) }))
            }
        }
        val url = String.format(_envManager.checkValidCreateRockUrl, network.lowercase())
        val bodyJson: String
        try {
            bodyJson = _api.post(url, _envManager.apSignatureToken, body)
        } catch (ex: Exception) {
            throw CustomException("API fail")
        }
        val jsonData = Json.parseToJsonElement(bodyJson).jsonObject

        val code = jsonData["code"]?.jsonPrimitive?.int ?: throw CustomException("Invalid response: code not found")
        if (code != 0) {
            throw CustomException("Time out or error")
        }
        val message = jsonData["message"] ?: throw CustomException("Invalid response: message not found")
        val confirmed = message.jsonObject["confirmed"]
            ?: throw CustomException("Invalid response: confirmed not found")

        return confirmed.toString().toBoolean()
    }

    private fun addTransactionToDatabase(
        uid: Int,
        tx: String,
        listHeroId: List<Int>,
        amount: Float,
        network: DataType
    ) {
        _gameDataAccessPostgreSql.logCreateRock(uid, tx, listHeroId, amount, network, Status.PENDING.name)
    }

    private fun checkDatabaseGuard(uid: Int, tx: String, network: DataType): Boolean {
        return _gameDataAccessPostgreSql.checkValidCreateRock(uid, tx, network)
    }

    private fun updateStatus(uid: Int, tx: String, network: DataType, status: Status) {
        _gameDataAccessPostgreSql.updateStatusCreateRock(uid, tx, network, status.name)
    }

    private fun getCommandUpgradeShield(walletAddress: String, heroId: Int, network: String): Pair<Int, String> {
        val body = Json.run {
            buildJsonObject {
                put("wallet_address", walletAddress)
                put("hero_id", heroId)
            }
        }
        val url = String.format(_envManager.apSignatureCmdUpdateShieldUrl, network.lowercase())

        val bodyJson: String
        try {
            bodyJson = _api.post(url, _envManager.apSignatureToken, body)
        } catch (ex: Exception) {
            throw CustomException("API fail")
        }
        val jsonData = Json.parseToJsonElement(bodyJson).jsonObject

        val data = jsonData["data"] ?: throw CustomException("Invalid response: data not found")
        val nonce = data.jsonObject["nonce"]
            ?: throw CustomException("Invalid response: nonce not found")
        val signature = data.jsonObject["signature"]
            ?: throw CustomException("Invalid response: signature not found")

        return Pair(nonce.toString().toInt(), signature.jsonPrimitive.content)
    }
}