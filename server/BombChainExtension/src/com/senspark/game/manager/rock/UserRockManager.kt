package com.senspark.game.manager.rock

import com.senspark.common.utils.ILogger
import com.senspark.game.api.IRestApi
import com.senspark.game.api.OkHttpRestApi
import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.hero.IHeroBuilder
import com.senspark.game.data.model.nft.BlockchainHeroDetails
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.db.IRewardDataAccess
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.EnumConstants.HeroType
import com.senspark.game.declare.SFSField
import com.senspark.game.declare.customEnum.ChangeRewardReason
import com.senspark.game.exception.CustomException
import com.senspark.game.extension.coroutines.ICoroutineScope
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IUsersManager
import com.senspark.game.manager.hero.IUserHeroFiManager
import com.senspark.game.pvp.HandlerCommand
import com.senspark.game.service.IHeroUpgradeShieldManager
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    private val _coroutineScope: ICoroutineScope,
    private val _usersManager: IUsersManager,
    private val _logger: ILogger,
) : IUserRockManager {

    override lateinit var convertHeroRockConfig: Map<Int, RockAmount>
    private val _pendingTransactions: ConcurrentHashMap<String, Int> = ConcurrentHashMap(mapOf())
    private val _api: IRestApi = OkHttpRestApi()

    private val _createRockTag = "[CREATE_ROCK]"
    private val _upgradeShieldVerifyTag = "[UPGRADE_SHIELD_VERIFY]"
    private val _upgradeShieldPollIntervalMs = 5_000L
    private val _upgradeShieldPollMaxAttempts = 12

    override fun initialize() {
        convertHeroRockConfig = _shopDataAccess.loadBurnHeroConfig()
        val missing = (0..9).filterNot { it in convertHeroRockConfig }
        check(missing.isEmpty()) {
            "config_burn_hero missing rarities: $missing. Apply rarity 6-9 migration."
        }
    }

    override fun setConfig(convertHeroRockConfig: Map<Int, RockAmount>) {
        this.convertHeroRockConfig = convertHeroRockConfig
    }

    override fun upgradeShieldLevel(userController: IUserController, hero: Hero): Pair<Int, String> {
        val walletAddress = userController.walletAddress
        if (walletAddress.isNullOrEmpty()) {
            throw CustomException("Only support BHero")
        }
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

        return getCommandUpgradeShield(walletAddress, hero.heroId, userController.dataType.name)
    }

    override fun upgradeShieldLevelV3(
        userController: IUserController,
        hero: Hero,
        debugFakePush: Boolean,
    ): Pair<Int, String> {
        val expectedLevel = hero.shield.level + 1
        val result = upgradeShieldLevel(userController, hero)
        startVerifyUpgradeShield(
            uid = userController.userId,
            heroId = hero.heroId,
            expectedLevel = expectedLevel,
            dataType = userController.dataType,
            debugFakePush = debugFakePush,
        )
        return result
    }

    private fun startVerifyUpgradeShield(
        uid: Int,
        heroId: Int,
        expectedLevel: Int,
        dataType: DataType,
        debugFakePush: Boolean,
    ) {
        _coroutineScope.scope.launch(Dispatchers.IO) {
            try {
                if (debugFakePush) {
                    _logger.log("$_upgradeShieldVerifyTag DEBUG fake push uid=$uid heroId=$heroId (level unchanged)")
                    delay(2_000L)
                    pushFakeUpgradeResponse(uid, heroId, dataType)
                    return@launch
                }
                repeat(_upgradeShieldPollMaxAttempts) {
                    delay(_upgradeShieldPollIntervalMs)
                    val onChainDetails = fetchHeroDetailsOnChain(heroId, dataType)
                    if (onChainDetails != null && onChainDetails.shieldLevel == expectedLevel) {
                        applyUpgradeAndPush(uid, heroId, expectedLevel, dataType, onChainDetails)
                        return@launch
                    }
                }
                _logger.log("$_upgradeShieldVerifyTag Timed out uid=$uid heroId=$heroId expected=$expectedLevel")
                pushUpgradeError(uid, dataType, "Upgrade verification timed out")
            } catch (e: Exception) {
                _logger.error("$_upgradeShieldVerifyTag Error uid=$uid heroId=$heroId: ${e.message}", e)
                pushUpgradeError(uid, dataType, "Upgrade verification error")
            }
        }
    }

    private fun pushFakeUpgradeResponse(uid: Int, heroId: Int, dataType: DataType) {
        val controller = _usersManager.getUserController(uid, dataType, EnumConstants.Landing.TREASURE)
        val heroFiManager = controller?.masterUserManager?.heroFiManager
        val hero = heroFiManager?.getHero(heroId, HeroType.FI)
        if (controller == null || heroFiManager == null || hero == null) {
            _logger.log("$_upgradeShieldVerifyTag DEBUG fake push skipped (offline or hero missing) uid=$uid heroId=$heroId")
            return
        }
        val payload = SFSObject()
        payload.putSFSObject(SFSField.Bomber, heroFiManager.heroToSfsObject(hero))
        payload.putSFSArray(SFSField.Rewards, controller.masterUserManager.blockRewardManager.toSfsArrays())
        controller.sendDataEncryption(HandlerCommand.UpgradeShieldLevelResponse, payload, true)
        _logger.log("$_upgradeShieldVerifyTag DEBUG pushed fake uid=$uid heroId=$heroId currentLevel=${hero.shield.level}")
    }

    private fun fetchHeroDetailsOnChain(heroId: Int, dataType: DataType): BlockchainHeroDetails? {
        val url = String.format(_envManager.apBlockchainHeroDetailsUrl, heroId.toString(), dataType.name.lowercase())
        val bodyJson = try {
            _api.get(url)
        } catch (e: Exception) {
            _logger.log("$_upgradeShieldVerifyTag fetch hero_details failed heroId=$heroId: ${e.message}")
            return null
        }
        val jsonData = Json.parseToJsonElement(bodyJson).jsonObject
        val code = jsonData["code"]?.jsonPrimitive?.intOrNull ?: return null
        if (code != 0) return null
        val message = jsonData["message"]?.jsonArray ?: return null
        val detailsHex = message.firstOrNull()?.jsonPrimitive?.contentOrNull ?: return null
        if (detailsHex.isEmpty() || detailsHex == "0") return null
        return BlockchainHeroDetails(detailsHex, dataType)
    }

    private fun applyUpgradeAndPush(
        uid: Int,
        heroId: Int,
        expectedLevel: Int,
        dataType: DataType,
        newDetails: BlockchainHeroDetails,
    ) {
        val controller = _usersManager.getUserController(uid, dataType, EnumConstants.Landing.TREASURE)
        val heroFiManager = controller?.masterUserManager?.heroFiManager
        val hero = heroFiManager?.getHero(heroId, HeroType.FI)
        if (controller == null || heroFiManager == null || hero == null) {
            _logger.log("$_upgradeShieldVerifyTag User offline or hero not loaded, skip push uid=$uid heroId=$heroId")
            return
        }
        // Swap in fresh on-chain details so gen_id reflects the new shieldLevel,
        // not just hero.shield.level (which heroToSfsObject doesn't write back into gen_id).
        hero.updateDetails(newDetails)
        hero.updateShieldLevel(expectedLevel)
        _gameDataAccessPostgreSql.updateHeroDetails(uid, dataType, hero)

        val payload = SFSObject()
        payload.putSFSObject(SFSField.Bomber, heroFiManager.heroToSfsObject(hero))
        payload.putSFSArray(SFSField.Rewards, controller.masterUserManager.blockRewardManager.toSfsArrays())
        controller.sendDataEncryption(HandlerCommand.UpgradeShieldLevelResponse, payload, true)
        _logger.log("$_upgradeShieldVerifyTag Pushed success uid=$uid heroId=$heroId level=$expectedLevel")
    }

    private fun pushUpgradeError(uid: Int, dataType: DataType, message: String) {
        val controller = _usersManager.getUserController(uid, dataType, EnumConstants.Landing.TREASURE) ?: return
        val payload = SFSObject()
        payload.putInt("code", 100)
        payload.putUtfString("message", message)
        controller.sendDataEncryption(HandlerCommand.UpgradeShieldLevelResponse, payload, true)
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
        val localRockReceive = sumRockReceive(userHeroFiManager, listIdHero)
        addTransactionToDatabase(uid, tx, listIdHero, localRockReceive, network)
        userHeroFiManager.syncBomberMan()

        try {
            val heroDetails = checkValidTransaction(tx, listIdHero, walletAddress, network.name)
            if (heroDetails != null) {
                // The burn event carries the details of every hero, including ones this server never
                // synced (a third-party client can burn without the user ever logging in). The local
                // and database prices only cover synced heroes, so use them just for burns older than
                // the CreateRockDetails contract upgrade.
                var totalRockReceive: Float
                if (heroDetails.isNotEmpty()) {
                    totalRockReceive = sumRockFromDetails(heroDetails, network)
                    _logger.log("$_createRockTag tx=$tx uid=$uid heroes=${heroDetails.size} rock=$totalRockReceive local=$localRockReceive")
                } else {
                    totalRockReceive = localRockReceive
                    if (totalRockReceive == 0f) {
                        val listHero = _heroBuilder.getHeroFiFromDatabase(network, listIdHero, HeroType.FI.value)
                        val listRockReceive = listHero.map {
                            if (it.isHeroS)
                                convertHeroRockConfig[it.rarity]!!.heroS
                            else convertHeroRockConfig[it.rarity]!!.heroL
                        }
                        totalRockReceive = listRockReceive.sum()
                    }
                    _logger.log("$_createRockTag No on-chain details tx=$tx uid=$uid rock=$totalRockReceive (legacy burn)")
                }
                updateStatus(uid, tx, network, Status.DONE, totalRockReceive)
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

    private fun sumRockFromDetails(heroDetails: List<String>, network: DataType): Float {
        var result = 0f
        heroDetails.forEach {
            val details = BlockchainHeroDetails(it, network)
            val config = convertHeroRockConfig[details.rarity]
            if (config == null) {
                _logger.log("$_createRockTag Missing burn config rarity=${details.rarity} heroId=${details.heroId}")
                throw CustomException("Invalid hero data")
            }
            result += if (details.isHeroS()) config.heroS else config.heroL
        }
        return result
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

    /**
     * Returns the burned heroes' on-chain details when the transaction is confirmed, null when it is
     * rejected. The list is empty for burns older than the CreateRockDetails contract upgrade.
     */
    private fun checkValidTransaction(
        tx: String,
        listIdHero: List<Int>,
        walletAddress: String,
        network: String
    ): List<String>? {
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
            bodyJson = _api.post(url, body)
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
        if (!confirmed.toString().toBoolean()) {
            return null
        }

        val heroDetails = message.jsonObject["hero_details"]?.jsonArray ?: return emptyList()
        val details = heroDetails.map { it.jsonPrimitive.content }
        if (details.size != listIdHero.size) {
            _logger.log("$_createRockTag Details mismatch tx=$tx heroes=${listIdHero.size} details=${details.size}")
            throw CustomException("Invalid hero data")
        }
        return details
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

    private fun updateStatus(uid: Int, tx: String, network: DataType, status: Status, amount: Float? = null) {
        _gameDataAccessPostgreSql.updateStatusCreateRock(uid, tx, network, status.name, amount)
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

        val message = jsonData["message"] ?: throw CustomException("Invalid response: message not found")
        val nonce = message.jsonObject["nonce"]
            ?: throw CustomException("Invalid response: nonce not found")
        val signature = message.jsonObject["signature"]
            ?: throw CustomException("Invalid response: signature not found")

        return Pair(nonce.toString().toInt(), signature.jsonPrimitive.content)
    }
}