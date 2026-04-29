package com.senspark.game.controller

import com.senspark.common.cache.ICacheService
import com.senspark.common.pvp.IMatchUserInfo
import com.senspark.common.service.IServiceLocator
import com.senspark.common.utils.IServerLogger
import com.senspark.game.api.BlockchainHeroResponse
import com.senspark.game.api.IBlockchainDatabaseManager
import com.senspark.game.api.IVerifyAdApiManager
import com.senspark.game.constant.CachedKeys
import com.senspark.game.data.PvPData
import com.senspark.game.data.UserPermissionsData
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.manager.pvp.IPvpRankingManager
import com.senspark.game.data.model.nft.HouseDetails
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.data.model.user.PvPRank
import com.senspark.game.data.model.user.RewardDetail
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.db.IRewardDataAccess
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.GameConstants
import com.senspark.game.declare.KickReason
import com.senspark.game.declare.SFSCommand.USER_INITIALIZED
import com.senspark.game.declare.SFSField
import com.senspark.game.declare.customEnum.ChangeRewardReason
import com.senspark.game.exception.CustomException
import com.senspark.game.extension.GlobalServices
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.extension.modules.ServerType
import com.senspark.game.handler.sol.EncryptionHelper
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IMasterUserManager
import com.senspark.game.manager.MasterUserManager
import com.senspark.game.pvp.DefaultPvPHistory
import com.senspark.game.pvp.IPvPHistory
import com.senspark.game.pvp.manager.IPvpQueueManager
import com.senspark.game.service.IAllHeroesFiManager
import com.senspark.game.service.IPvpDataAccess
import com.senspark.game.service.ServiceLocator
import com.senspark.game.user.IUserPermissions
import com.senspark.game.user.PvpUserController
import com.senspark.game.user.SkinChest
import com.senspark.game.user.UserPermissions
import com.senspark.game.utils.Extractor
import com.senspark.game.utils.Utils
import com.senspark.lib.data.manager.IGameConfigManager
import com.senspark.lib.db.ILibDataAccess
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.senspark.game.pvp.config.PvpWagerTier
import com.senspark.game.pvp.config.PvpWagerToken
import com.senspark.game.pvp.manager.PvpWagerManager
import com.senspark.game.exception.CustomException
import com.senspark.game.declare.ErrorCode
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import com.smartfoxserver.v2.extensions.SFSExtension
import java.lang.ref.WeakReference
import java.sql.Timestamp
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

class LegacyUserController(
    private val _extension: SFSExtension,
    private val _userInfo: IUserInfo,
    private val _services: GlobalServices
) : IUserController {
    override val locker = Any()
    override lateinit var masterUserManager: IMasterUserManager
    override val svServices = _services.get<ISvServicesContainer>().get(ServerType.BNB_POL)
    override val logger = svServices.get<IServerLogger>()

    private var _inGame = false
    private var _pvpUserController: PvpUserController? = null
    private val _needSave = mutableMapOf<EnumConstants.SAVE, Boolean>()
    private val _userPermissions: IUserPermissions = UserPermissions(
        // FIXME: nhanc18 temporarily disable this feature as it is not needed
        UserPermissionsData(
            createRoom = false,
            resetData = false,
            storyImmortal = false,
            storyOneHit = false,
            viewPvPDashboard = false
        )
    )
    private val _serviceLocator: IServiceLocator = ServiceLocator()
    private var _isActive: Boolean
    private var _requestId: Int = 0
    private var _checkPointDDos: Long = System.currentTimeMillis()
    private var _checkDDosSupply: Int = 0
    private var _checkPointIndex: Int = 0
    private var _pvpHistory: IPvPHistory? = null
    private var _pvpRank: PvPRank? = null

    private var _lastTimeCheckValidData: Date = Date()
    private var _lastPlayedHeroId: Long = 0
    private var _lastTimeSaveGame: Date = Date()

    private var _isDisposed = false
    private var _userRef: WeakReference<User>? = null
    private var _initializeStatus: InitializeStatus = InitializeStatus.NOT_INITIALIZED

    private val _gameDataAccess = _services.get<IGameDataAccess>()
    private val _libDataAccess = _services.get<ILibDataAccess>()
    private val _rewardDataAccess = _services.get<IRewardDataAccess>()
    private val _pvpDataAccess = _services.get<IPvpDataAccess>()
    private val _userDataAccess = _services.get<IUserDataAccess>()
    private val _envManager = _services.get<IEnvManager>()
    private val _cacheService = _services.get<ICacheService>()
    private val _gameConfigManager = _services.get<IGameConfigManager>()

    private val _pvpRankingManager = svServices.get<IPvpRankingManager>()
    private val _configItemManager = svServices.get<IConfigItemManager>()
    private val _allHeroesFiManager = svServices.get<IAllHeroesFiManager>()
    private val _verifyAdApi = svServices.get<IVerifyAdApiManager>()
    private val _blockchainDatabase = svServices.get<IBlockchainDatabaseManager>()
    private val _pvpQueue = svServices.get<IPvpQueueManager>()

    init {
        this._isActive = false
        this._checkPointDDos = System.currentTimeMillis()
        this._checkPointIndex = 0
        for (save in EnumConstants.SAVE.entries) {
            _needSave[save] = false
        }
    }

    override fun setUser(user: User) {
        if (_userRef != null) {
            _userRef?.clear()
        }
        _userRef = WeakReference(user)
    }

    override fun initDependencies(): Boolean {
        if (_initializeStatus == InitializeStatus.INITIALIZING) {
            return false
        }
        if (_initializeStatus == InitializeStatus.SUCCESS) {
            setActive(true)
            _inGame = true
            _extension.send(USER_INITIALIZED, SFSObject(), user)
            return true
        }

        _isDisposed = false
        try {
            masterUserManager = MasterUserManager(_userPermissions, createMediator(), _verifyAdApi)
            _serviceLocator.provide(masterUserManager.userStakeVipManager)
            _serviceLocator.provide(masterUserManager.userInventoryManager)
            _serviceLocator.provide(_userPermissions)

            setPvP(_gameDataAccess.queryPvP(userId))
            setPvPRank(
                _pvpRankingManager.getRanking(
                    _userInfo.displayName,
                    _userInfo.id,
                    _userInfo.dataType
                )
            )
            masterUserManager.userSubscriptionManager.takeSubscriptionRewards()
            leavePvPQueue()

            _pvpUserController = PvpUserController(
                _userInfo,
                _envManager,
                _configItemManager,
                masterUserManager.userInventoryManager
            )

            setActive(true)
            _inGame = true
            _initializeStatus = InitializeStatus.SUCCESS
            _allHeroesFiManager.addSubManager(
                _userInfo.id,
                _userInfo.dataType,
                masterUserManager.heroFiManager
            )
            sendDataEncryption(USER_INITIALIZED, SFSObject())
            logger.log("User $userName initialized")
            return true
        } catch (e: Exception) {
            logger.log("Error: ${e.message}")
            _initializeStatus = InitializeStatus.FAILED
            _isDisposed = true
            return false
        }
    }

    override fun isInitialized(): Boolean {
        return _initializeStatus == InitializeStatus.SUCCESS
    }

    fun isDisposed(): Boolean {
        return _isDisposed
    }

    override fun dispose() {
        if (_initializeStatus == InitializeStatus.INITIALIZING) {
            _initializeStatus = InitializeStatus.CANCELED
        }
        _isDisposed = true
        if (this::masterUserManager.isInitialized) {
            masterUserManager.userAdventureModeManager.endGameAndSaveData(
                EnumMap(EnumConstants.BLOCK_REWARD_TYPE::class.java),
                EnumConstants.MatchResult.OUT
            )
        }
        _allHeroesFiManager.removeSubManager(_userInfo.id, _userInfo.dataType)
        if (_inGame) {
            saveGame()
        }
        leavePvPQueue()
        _userRef?.clear()
    }

    override val user
        get():User? {
            return _userRef!!.get()
        }

    val userType
        get(): EnumConstants.UserType {
            return _userInfo.type
        }

    override val dataType
        get(): EnumConstants.DataType {
            return _userInfo.dataType
        }

    override fun isAirdropUser(): Boolean {
        return false;
    }

    fun setUserMode(userMode: EnumConstants.UserMode) {
        _userInfo.mode = userMode
    }

    val isTrial
        get(): Boolean {
            return _userInfo.type == EnumConstants.UserType.FI && _userInfo.mode == EnumConstants.UserMode.TRIAL
        }

    override fun setNeedSave(key: EnumConstants.SAVE) {
        _needSave[key] = true
        if (_gameConfigManager.countDownSaveUserData <= Utils.subTwoDates(
                _lastTimeSaveGame,
                Date(),
                TimeUnit.SECONDS
            )
        ) {
            saveGame()
        }
    }

    private fun generateHash(): String {
        val current = System.currentTimeMillis()
        val random = Utils.randInt(0, 10000)
        val result = "${current}${random}sv${_envManager.serverId}"
        saveHashToCached(result)
        _userInfo.setHash(result)
        return result
    }

    private fun getHash(): String {
        return _userInfo.hash
    }

    override val userId
        get(): Int {
            return _userInfo.id
        }

    override val userInfo
        get(): IUserInfo {
            return _userInfo
        }

    override val userName
        get(): String {
            return _userInfo.username
        }

    val isActive
        get(): Boolean {
            return _isActive
        }

    fun setActive(active: Boolean) {
        _isActive = active
    }

    fun increaseRequestId() {
        ++_requestId
    }

    val requestId
        get(): Int {
            return _requestId
        }

    fun saveGame() {
        if (!checkHash()) {
            disconnect(KickReason.CHEAT_LOGIN)
            return
        }
        if (!_needSave.containsValue(true)) {
            return
        }

        if (!validHouseAndHero()) {
            disconnect(KickReason.NEED_LOGIN_AGAIN)
            return
        }

        _lastTimeSaveGame = Date()
        saveEnergy()
        saveReward()
        masterUserManager.userBlockMapManager.saveMap(userId, _needSave)
    }

    override fun ban(isBan: Int, banReason: String, banExpired: Timestamp?) {
        if (isBan == 0) {
            _libDataAccess.updateUnBanUser(userId)
        } else {
            _libDataAccess.updateBanUser(userId, banReason, banExpired)
        }
    }

    fun hackDetect() {
        _libDataAccess.updateIsReview(userId, 1)
    }

    fun validHouseAndHero(): Boolean {
        if (_gameConfigManager.countDownCheckUserData <= Utils.subTwoDates(
                _lastTimeCheckValidData,
                Date(),
                TimeUnit.SECONDS
            )
        ) {
            _lastTimeCheckValidData = Date()
            return checkValidBomberMan() && checkValidHouse()
        }
        return true
    }

    fun checkValidBomberMan(): Boolean {
        val database = _blockchainDatabase.heroDatabase
        val detailList: List<BlockchainHeroResponse>
        try {
            detailList = database.query(_userInfo.id,_userInfo.username, _userInfo.dataType)
        } catch (ex: Exception) {
            // Cho nay khong quan trong lam nen return true de tranh truong hop API fail.
            return true
        }
        val heroes = masterUserManager.heroFiManager.getBombermans()
        return if (detailList.size != heroes.size) {
            false
        } else {
            detailList.all { e -> heroes[Extractor.parseHeroId(e.details.heroId, e.details.type)] != null }
        }
    }

    fun checkValidHouse(): Boolean {
        val database = _blockchainDatabase.houseDatabase
        val detailList: List<HouseDetails>
        try {
            detailList = database.query(_userInfo, _userInfo.dataType)
        } catch (ex: Exception) {
            // Cho nay khong quan trong lam nen return true de tranh truong hop API fail.
            return true
        }
        val userHouses = masterUserManager.houseManager.getUserHouses()
        val houseValid: Boolean = when {
            detailList.size != userHouses.size -> {
                false
            }

            else -> {
                detailList.all { e -> userHouses[e.houseId] != null }
            }
        }

        // Check house status: if no active house is found, set hero to sleep mode
        val activeHouse = masterUserManager.houseManager.activeHouse
        if (activeHouse != null && detailList.none { e -> e.houseId == activeHouse.houseId }) {
            val bombermanRest = masterUserManager.heroFiManager.housingHeroes.map { e ->
                masterUserManager.heroFiManager.setSleep(e)
                e
            }

            // Update database
            _gameDataAccess.updateBomberEnergyAndStage(userId, _userInfo.dataType, bombermanRest)
        }

        return houseValid
    }

    override fun checkHash(): Boolean {
        return true;
        // Temporarily comment out as hash is stored on redis using uid, and currently 5 networks share 1 uid, so the next network will overwrite the previous hash.
        //return getHashFromCached() == getHash()
    }

    override fun logOut() {
        masterUserManager.userDailyTaskManager.saveToDatabase()
        masterUserManager.updateLogoutMediator()
        masterUserManager.userDataManager.updateLogoutInfo()
    }

    override fun setUserInfo(userInfo: IUserInfo) {
        _userInfo.privateKeyRSA = userInfo.privateKeyRSA
        _userInfo.privateKeyRSAStr = userInfo.privateKeyRSAStr
        _userInfo.aesKey = userInfo.aesKey
    }

    private fun getHashFromCached(): String {
        var hash: String? = ""
        try {
            hash = _cacheService.getFromHash(CachedKeys.SV_USR_HASH, userId.toString())
            if (hash == null) {
                hash = _libDataAccess.getUserHash(userId)
                saveHashToCached(hash)
            }
        } catch (e: Exception) {
            logger.log("Hash error: ${e.message}")
        }
        return hash ?: ""
    }

    private fun saveHashToCached(hash: String) {
        _cacheService.setToHash(CachedKeys.SV_USR_HASH, userId.toString(), hash, 5.minutes)
    }

    private fun saveEnergy() {
        val saveKey = EnumConstants.SAVE.HERO_STATUS
        if (!_needSave[saveKey]!!) {
            return
        }
        _needSave[saveKey] = false

        val bbmLst = masterUserManager.heroFiManager.activeHeroes
        _gameDataAccess.updateBomberEnergyAndStage(userId, _userInfo.dataType, bbmLst)
        for (bbm in bbmLst) {
            bbm.onSaved()
        }
    }

    private fun saveReward() {
        try {
            val saveKey = EnumConstants.SAVE.REWARD
            if (!_needSave[saveKey]!!) {
                return
            }
            _needSave[saveKey] = false

            val userRw = masterUserManager.blockRewardManager
            val mapReward = userRw.getRewardsMining().toMutableMap()

            userRw.onSaved()

            // Add rewards to user and save to DB
            if (mapReward.isNotEmpty()) {
                val nerfRewards = listOf(
                    EnumConstants.BLOCK_REWARD_TYPE.BCOIN,
                    EnumConstants.BLOCK_REWARD_TYPE.SENSPARK,
                    EnumConstants.BLOCK_REWARD_TYPE.MSPc
                )
                nerfRewards.forEach { rewardType ->
                    if (mapReward.containsKey(rewardType)) {
                        val rewardDetail = mapReward[rewardType]!!
                        var value = rewardDetail.value
                        value *= getRewardPercent()
                        rewardDetail.value = value
                        mapReward[rewardType] = rewardDetail
                    }
                }

                val rewardDetails = mutableListOf<RewardDetail>()
                // Save to DB
                for (reward in mapReward.entries) {
                    val rewardDetail = reward.value
                    _rewardDataAccess.addUserBlockReward(
                        userId,
                        rewardDetail.blockRewardType,
                        rewardDetail.dataType,
                        rewardDetail.value,
                        rewardDetail.forControlValue,
                        ChangeRewardReason.SAVE_GAME
                    )
                    rewardDetails.add(rewardDetail)
                }

            }
        } catch (e: Exception) {
            logger.error(e)
        }
    }

    fun getRewardPercent(): Float {
        return 1f
    }

    fun checkDdos(): Boolean {
        if (isStartingServer()) return false

        _checkPointIndex++
        if (_checkPointIndex == _checkDDosSupply) {
            val now = System.currentTimeMillis()
            val duration = now - _checkPointDDos
            _checkPointIndex = 0
            _checkPointDDos = now
            if (duration < GameConstants.CHECK_DDOS_DURATION) {
                logger.log("Ddos detected: $userName")
                disconnect(KickReason.HACK_CHEAT)
                return true
            }
        }
        return false
    }

    // Calculate maximum number of requests allowed from client
    fun recalculateDDosSupply() {
        val heroesActive = masterUserManager.heroFiManager.activeHeroes
        var amountBombs = 0
        for (bm in heroesActive) {
            amountBombs += bm.bombCount
        }

        val powDDos = GameConstants.CHECK_DDOS_DURATION / GameConstants.BOMB_EXPLODE_DURATION
        val maxDDos = amountBombs * powDDos * 2 // Allow 100% variance
        _checkDDosSupply = maxOf(maxDDos, GameConstants.CHECK_DDOS_SUPPLY)
    }

    override fun saveGameAndLoadReward() {
        // Save game and rewards
        saveGame()
        // Load saved rewards
        masterUserManager.blockRewardManager.loadUserBlockReward()
    }

    override fun loadReward() {
        masterUserManager.blockRewardManager.loadUserBlockReward()
    }

    /**
     * If user logs in via account, certain contract-related features will be limited.
     */
    fun disableWhileLoginByAccount(): Boolean {
        return _userInfo.type == EnumConstants.UserType.TR
    }

    override val pvPConfig
        get(): ISFSObject {
            val result = SFSObject()
            result.putInt("ticket_price", _gameConfigManager.pvpTicketPrice)
            result.putFloat("reward_fee", _gameConfigManager.pvpRewardFee)
            result.putBool("season_valid", true)
            result.putInt("pvp_number_shard", _gameConfigManager.openSkinChestCost.toInt())
            result.putBool("is_white_list", false)
            return result
        }

    override fun reloadPvpRanking() {
        _pvpRankingManager.clearDataForOneUser(_userInfo.id)
        setPvPRank(
            _pvpRankingManager.getRanking(
                _userInfo.displayName,
                _userInfo.id,
                _userInfo.dataType
            )
        )
    }

    @Throws(Exception::class)
    override fun joinPvpQueue(
        mode: Int,
        matchId: String,
        test: Boolean,
        heroId: Int,
        boosters: List<Int>,
        pings: Map<String, Int>,
        avatar: Int,
        gameMode: Int,
        wagerMode: Int,
        wagerTier: Int,
        wagerToken: Int
    ) {
        if (_pvpUserController == null) {
            return
        }
        val user = _userRef?.get() ?: return
        val hero = masterUserManager.heroTRManager.getHero(heroId)
        masterUserManager.userPvPBoosterManager.loadFromDb()
        val boosterManager = masterUserManager.userPvPBoosterManager
        val boostersMap = mutableMapOf<Int, Int>()
        for (id in boosters) {
            boosterManager.chooseBooster(id, true)
            val booster = boosterManager.getBooster(id)
            if (booster != null) {
                boostersMap[id] = booster.quantity
            }
        }
        _lastPlayedHeroId = heroId.toLong()
        
        val wagerTokenEnum = PvpWagerToken.from(wagerToken)
        val wagerTierEnum = PvpWagerTier.from(wagerTier)

        if (wagerMode == 1) {
            if (wagerTokenEnum == PvpWagerToken.NONE) {
                throw CustomException("Invalid wager token", ErrorCode.BAD_REQUEST)
            }
            if (wagerTokenEnum.network != _userInfo.dataType) {
                throw CustomException("Token network mismatch: ${wagerTokenEnum.network} != ${_userInfo.dataType}", ErrorCode.BAD_REQUEST)
            }
        }
        
        // DELETED REDUNDANT DEDUCTION (Centralized in MatchManager Escrow)

        val info: IMatchUserInfo =
            _pvpUserController!!.getMatchInfo(this, matchId, mode, test, hero, boostersMap, avatar)

        _cacheService.setToHash(
            CachedKeys.AES_KEY,
            userName,
            Base64.getEncoder().encodeToString(userInfo.aesKey.encoded),
            15.minutes
        )
        _pvpQueue.join(
            user = user,
            username = userName,
            pings = pings,
            info = info,
            aesKey = userInfo.aesKey,
            gameMode = gameMode,
            wagerMode = wagerMode,
            wagerTier = wagerTier,
            wagerToken = wagerToken,
            network = _userInfo.dataType.name
        )
    }

    override val pvpRank
        get(): PvPRank {
            return _pvpRank!!
        }

    private fun setPvP(data: PvPData) {
        _lastPlayedHeroId = data.lastPlayedHero
    }

    fun setPvPRank(rank: PvPRank) {
        _pvpRank = rank
    }

    override fun updatePvpRanking(point: Int, match: Int, win: Int) {
        _pvpRank?.update(point, match, win)
    }

    @Throws(CustomException::class)
    fun userStake(type: EnumConstants.BLOCK_REWARD_TYPE, amount: Float, allIN: Boolean): ISFSObject {
        saveGame()
        val result = masterUserManager.userStakeManager.stake(userName, type, amount, allIN)
        masterUserManager.blockRewardManager.loadUserBlockReward()
        masterUserManager.userStakeVipManager.reload()
        return result
    }

    /**
     * Get user stake info (can also withdraw)
     *
     * @param isWithdraw whether to withdraw. If false, only calculate info without withdrawing.
     * @return User stake info
     */
    @Throws(Exception::class)
    fun userWithdrawStake(isWithdraw: Boolean): ISFSObject {
        // If withdrawing, claim remaining VIP stake rewards
        if (isWithdraw) {
            masterUserManager.userStakeVipManager.claimRemainingReward()
        }
        val result = masterUserManager.userStakeManager.withdrawStake(userName, isWithdraw)
        if (isWithdraw) {
            masterUserManager.blockRewardManager.loadUserBlockReward()
            masterUserManager.userStakeVipManager.reload()
        }
        return result
    }

    override fun getPvPHistory(at: Int, count: Int): ISFSArray {
        return getPvPHistoryManager().toSFSArray(at, count)
    }

    private fun getPvPHistoryManager(): IPvPHistory {
        if (_pvpHistory == null) {
            _pvpHistory = DefaultPvPHistory(walletAddress) { message ->
                message?.let {
                    logger.log(it)
                }
            }
        }
        _pvpHistory?.clear()
        val logs = _pvpDataAccess.queryLogPlayPvP(walletAddress)
        _pvpHistory?.setItems(logs)
        return _pvpHistory!!
    }

    override val lastPlayedPvPHeroId
        get(): Long {
            return _lastPlayedHeroId
        }

    override val walletAddress
        get(): String {
            return userName
        }

    override fun countUserRanked(): Int {
        return _pvpRankingManager.getTotalCount()
    }

    override val pvPRanking
        get(): ISFSObject {
            _pvpRank = _pvpRankingManager.getRanking(_userInfo.displayName, userId, _userInfo.dataType)
            return _pvpRank!!.toSFSObject()
        }

    override val pvPRankingList
        get(): ISFSArray {
            return _pvpRankingManager.toSFSArray()
        }

    @Throws(CustomException::class)
    fun checkChestReward(price: Float, type: Int): Boolean {
        masterUserManager.blockRewardManager.loadUserBlockReward()
        val deposited = masterUserManager.blockRewardManager.getRewardValue(
            EnumConstants.BLOCK_REWARD_TYPE.valueOf(type),
            EnumConstants.BLOCK_REWARD_TYPE.valueOf(type).getDataType(dataType)
        )
        val reward = masterUserManager.blockRewardManager.getRewardValue(
            EnumConstants.BLOCK_REWARD_TYPE.valueOf(type).swapDepositedOrReward(),
            EnumConstants.BLOCK_REWARD_TYPE.valueOf(type).getDataType(dataType)
        )
        return deposited + reward >= price
    }

    fun logPvpBooster(type: String, itemId: Int, boosterPrice: Int) {
        _userDataAccess.logPvpBooster(
            type,
            itemId,
            _configItemManager.getItem(itemId).name,
            userId,
            boosterPrice
        )
    }

    override fun leavePvPQueue(): Boolean {
        val result: Boolean = _pvpQueue.leave(userName)
        if (!result) {
            logger.log("Could not leave pvp queue")
            return false
        }
        return true
    }

    @Throws(CustomException::class)
    fun rename(newName: String, feeRewardType: EnumConstants.BLOCK_REWARD_TYPE) {
        saveGame()
        val renameFee = 50f
        val depositHaving = masterUserManager.blockRewardManager.getRewardValue(feeRewardType.swapDepositedOrReward())
        _userDataAccess.updateAccountName(
            _userInfo.dataType,
            userName,
            newName,
            renameFee,
            minOf(renameFee, depositHaving),
            feeRewardType
        )
        _userInfo.setName(newName)
        _pvpRank?.name = newName
        masterUserManager.blockRewardManager.loadUserBlockReward()
    }

    override val fullName
        get(): String {
            return _userInfo.name!!
        }

    @Throws(Exception::class)
    override fun openSkinChest(): SkinChest {
        if (!checkHash()) {
            disconnect(KickReason.CHEAT_LOGIN)
            throw Exception("Invalid hash")
        }
        return masterUserManager.userInventoryManager.openSkinChest()
    }

    override val userPermissions
        get(): IUserPermissions {
            return _userPermissions
        }

    override val serviceLocator
        get(): IServiceLocator {
            return _serviceLocator
        }

    fun deleteAccount() {
        _userDataAccess.deleteUserAccount(userId)
    }

    override fun disconnect(reason: KickReason) {
        val user = _userRef?.get() ?: return
        _isActive = false
        _extension.api.disconnectUser(user, reason)
    }

    override fun send(cmdName: String, data: ISFSObject, log: Boolean) {
        val user = _userRef?.get()
        if (user == null) {
            logger.log("User $userName is disposed")
            return
        }
        _extension.send(cmdName, data, user)
        if (log) {
            log(cmdName) { "Send no encrypt: ${data.toJson()}" }
        }
    }

    override fun sendDataEncryption(cmdName: String, data: ISFSObject, log: Boolean) {
        val user = _userRef?.get()
        if (user == null) {
            logger.log("User $userName is disposed")
            return
        }
        val encryptedData = EncryptionHelper.encryptToBytes(data.toJson(), userInfo.aesKey)
        val responseData = SFSObject()
        responseData.putByteArray(SFSField.Data, encryptedData)
        _extension.send(cmdName, responseData, user)
        if (log) {
            log(cmdName) { "Send encrypted: ${data.toJson()}" }
        }
    }

    override fun verifyAndUpdateUserHash(): Boolean {
        val newHash = generateHash()
        return _libDataAccess.updateHash(userId, newHash)
    }

    private fun tryToKickAndWriteLogHack(type: Int, data: String): Boolean {
        logger.log("HACKING $userName $data")
        if (_gameConfigManager.isKickWhenHack == 1) {
            disconnect(KickReason.HACK_CHEAT)
            return true
        }
        return false
    }

    private fun createMediator(): UserControllerMediator {
        val lastLogOut: () -> Instant? = {
            _userInfo.lastLogout
        }
        val saveImmediately: (EnumConstants.SAVE) -> Unit = { kind ->
            setNeedSave(kind)
            saveGame()
        }
        val setUsedPvpBoosterToDatabase: (Int) -> Unit = { itemId ->
            logPvpBooster("used", itemId, 0)
        }
        val isCheatByMultipleLogin: () -> Boolean = {
            if (!checkHash()) {
                disconnect(KickReason.CHEAT_LOGIN)
                true
            } else {
                false
            }
        }
        return UserControllerMediator(
            // fields:
            _userInfo.id,
            _userInfo.dataType,
            _userInfo.username,
            _userInfo.type,
            _userInfo.deviceType,
            _userInfo.platform,
            _services,
            svServices,
            logger,

            // properties:
            lastLogOut,

            // function
            ::setNeedSave,
            saveImmediately,
            ::tryToKickAndWriteLogHack,
            setUsedPvpBoosterToDatabase,
            ::saveGameAndLoadReward,
            isCheatByMultipleLogin,
            ::sendDataEncryption
        )
    }

    private fun isStartingServer(): Boolean {
        return System.currentTimeMillis() - _envManager.serverStartTime < 3600000
    }

    private fun log(serverCommand: String, msg: () -> String) {
        val tag = "[OUT] ${userName}: $serverCommand"
        logger.log2(tag, msg)
    }
}

enum class InitializeStatus {
    NOT_INITIALIZED,
    INITIALIZING,
    SUCCESS,
    FAILED,
    CANCELED
}
