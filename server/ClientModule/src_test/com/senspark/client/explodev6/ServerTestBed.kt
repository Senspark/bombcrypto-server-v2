package com.senspark.client.explodev6

import com.senspark.common.service.IScheduler
import com.senspark.common.service.ServiceContainer
import com.senspark.common.utils.AppStage
import com.senspark.common.utils.IServerLogger
import com.senspark.game.controller.IUserController
import com.senspark.game.controller.MapData
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.manager.block.IBlockConfigManager
import com.senspark.game.data.manager.block.IBlockDropByDayManager
import com.senspark.game.data.manager.block.IBlockRewardDataManager
import com.senspark.game.data.model.config.BlockConfig
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.IHeroDetails
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.GameConstants
import com.senspark.game.extension.GlobalServices
import com.senspark.game.extension.MainGameExtension
import com.senspark.game.extension.ServerServices
import com.senspark.game.extension.coroutines.ICoroutineScope
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.handler.airdropUser.GetBombTargetHandler
import com.senspark.game.handler.airdropUser.StartPlantBombHandler
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.handler.sol.EncryptionHelper
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IMasterUserManager
import com.senspark.game.manager.IUsersManager
import com.senspark.game.manager.blockMap.UserBlockMapManagerImpl
import com.senspark.game.manager.blockReward.IUserBlockRewardManager
import com.senspark.game.manager.hero.IUserHeroFiManager
import com.senspark.game.manager.stake.IHeroStakeManager
import com.senspark.game.manager.treasureHuntV2.ITreasureHuntV2Manager
import com.senspark.game.utils.AesEncryption
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKey

// Runs real explode-v6 handlers + UserBlockMapManagerImpl in-process; DB/heroes/config are mocked.
// Requests go through the real encrypt/decrypt round-trip to catch field-type mismatches.
class ServerTestBed(
    val map: MapData,
    val dataType: DataType = DataType.BSC,
    val userId: Int = 1,
    val userName: String = "explode-v6-test",
) {
    data class ServerResponse(
        val command: String,
        val requestId: Int,
        val errorCode: Int?,
        val data: ISFSObject?,
    ) {
        val isError get() = errorCode != null
        fun require(): ISFSObject = data ?: error("expected success, got error code $errorCode")
    }

    val aesKey: SecretKey = AesEncryption.generateKey()

    val outbox = mutableListOf<ServerResponse>()

    val hackLogs = mutableListOf<Pair<Int, String>>()

    // Pushes are stored unencrypted (no request to mismatch against).
    data class Push(val command: String, val data: ISFSObject)

    // PVE_NEW_MAP and RESPONSE_EXPLODE arrive here.
    val pushes = mutableListOf<Push>()

    val saves = mutableListOf<EnumConstants.SAVE>()

    // Handlers swallow exceptions, so capture logged errors to tell a crash from silence.
    val loggedErrors = mutableListOf<String>()

    val gameConfig: IGameConfigManager = mockk(relaxed = true)
    val gameDataAccess: IGameDataAccess = mockk(relaxed = true)
    val treasureHuntV2Manager: ITreasureHuntV2Manager = mockk(relaxed = true)
    val heroStakeManager: IHeroStakeManager = mockk(relaxed = true)
    val blockRewardDataManager: IBlockRewardDataManager = mockk(relaxed = true)
    val blockRewardManager: IUserBlockRewardManager = mockk(relaxed = true)
    val blockDropByDayManager: IBlockDropByDayManager = mockk(relaxed = true)
    val blockConfigManager: IBlockConfigManager = mockk(relaxed = true)
    val logger: IServerLogger = mockk(relaxed = true)

    val controller: IUserController = mockk(relaxed = true)
    val heroFiManager: IUserHeroFiManager = mockk(relaxed = true)
    val masterUserManager: IMasterUserManager = mockk(relaxed = true)

    private val _heroes = mutableMapOf<Int, Hero>()

    // Differs from [map] after createNewMap() replaces _mapData.
    val currentMap: MapData
        get() {
            // _mapData is LazyMutable-delegated, so read via its generated getter.
            val getter = UserBlockMapManagerImpl::class.java.getDeclaredMethod("get_mapData")
            getter.isAccessible = true
            return getter.invoke(blockMap) as MapData
        }

    val blockMap: UserBlockMapManagerImpl

    val getBombTargetHandler: GetBombTargetHandler
    val startPlantBombHandler: StartPlantBombHandler

    val scheduler = FakeScheduler()

    private val _sfsUser: User = mockk(relaxed = true)
    private var _nextRequestId = 1

    init {
        TestServerRuntime.install()

        configureDefaults()

        // Fresh containers per bed, since ServiceContainer caches the first instance.
        val globalServices: GlobalServices = ServiceContainer("test-bed-global")
        val serverServices: ServerServices = ServiceContainer("test-bed-server")
        val dataAccessManager: IDataAccessManager = mockk(relaxed = true)
        every { dataAccessManager.gameDataAccess } returns gameDataAccess
        globalServices.register(IBlockConfigManager::class) { blockConfigManager }
        globalServices.register(IDataAccessManager::class) { dataAccessManager }
        globalServices.register(IBlockDropByDayManager::class) { blockDropByDayManager }
        globalServices.register(IBlockRewardDataManager::class) { blockRewardDataManager }
        globalServices.register(IGameConfigManager::class) { gameConfig }
        globalServices.register(IScheduler::class) { scheduler }
        serverServices.register(ITreasureHuntV2Manager::class) { treasureHuntV2Manager }
        serverServices.register(IHeroStakeManager::class) { heroStakeManager }

        val mediator = UserControllerMediator(
            userId = userId,
            dataType = dataType,
            userName = userName,
            walletAddress = null,
            userType = EnumConstants.UserType.FI,
            isOriginallyFi = true,
            deviceType = EnumConstants.DeviceType.WEB,
            platform = EnumConstants.Platform.WebPC,
            services = globalServices,
            svServices = serverServices,
            logger = logger,
            lastLogOut = { null },
            saveLater = { saves.add(it) },
            saveImmediately = { saves.add(it) },
            tryToKickAndWriteLogHack = { type, data ->
                hackLogs.add(type to data)
                gameConfig.isKickWhenHack == 1
            },
            setUsedPvpBoosterToDatabase = { },
            saveGameAndLoadReward = { },
            isCheatByMultipleLogin = { false },
            sendDataEncryption = { cmd, data, _ -> pushes.add(Push(cmd, data)) },
        )

        every { gameDataAccess.loadSingleMapData(any(), any(), any()) } returns map

        blockMap = UserBlockMapManagerImpl(mediator, blockRewardManager, heroFiManager)

        every { masterUserManager.userBlockMapManager } returns blockMap
        every { masterUserManager.heroFiManager } returns heroFiManager
        every { heroFiManager.getHero(any<Int>(), any<DataType>()) } answers { _heroes[firstArg()] }

        val userInfo: IUserInfo = mockk(relaxed = true)
        every { userInfo.aesKey } returns aesKey
        every { controller.userInfo } returns userInfo
        every { controller.userId } returns userId
        every { controller.userName } returns userName
        every { controller.dataType } returns dataType
        every { controller.landing } returns EnumConstants.Landing.TREASURE
        every { controller.logger } returns logger
        every { controller.masterUserManager } returns masterUserManager
        every { controller.isInitialized() } returns true
        every { controller.user } returns null
        every { controller.setNeedSave(any()) } answers { saves.add(firstArg()) }

        every { logger.error(any<String>()) } answers { loggedErrors.add(firstArg()) ; Unit }
        every { logger.error(any<Exception>()) } answers { loggedErrors.add(firstArg<Exception>().toString()) ; Unit }
        every { logger.error(any<String>(), any<Exception>()) } answers {
            loggedErrors.add("${firstArg<String>()}: ${secondArg<Exception>()}")
            Unit
        }

        val cmd = slot<String>()
        val payload = slot<ISFSObject>()
        every { controller.send(capture(cmd), capture(payload), any()) } answers {
            outbox.add(decodeResponse(cmd.captured, payload.captured))
        }

        TestServerRuntime.bind(controller)

        getBombTargetHandler = GetBombTargetHandler()
        startPlantBombHandler = StartPlantBombHandler()
    }

    // Key mirrors UserBlockMapManagerImpl.scheduleFuse. Returns whether a fuse was pending.
    fun fireFuse(heroId: Int, bombNo: Int, col: Int, row: Int): Boolean =
        scheduler.fire("bombfuse-$userId-$dataType-$heroId-$bombNo-$col-$row")

    // Move-time tolerance is wide open by default; PlantMoveSpeedTest tightens it.
    private fun configureDefaults() {
        every { gameConfig.isCheckPlantMoveSpeed } returns true
        every { gameConfig.isRejectPlantTooFast } returns true
        every { gameConfig.plantMoveSpeedToleranceMs } returns 600_000
        every { gameConfig.plantMoveSpeedMultiplier } returns 1.15f
        every { gameConfig.plantMoveSpeedMin } returns 1f
        every { gameConfig.isKickWhenHack } returns 0
        every { gameConfig.timeBombExplode } returns 0
        every { gameConfig.blockDensity } returns 0.5f
        every { gameConfig.maxTitleset } returns 1
        every { gameConfig.minStakeBcoinTHV1 } returns List(10) { Int.MAX_VALUE }
        every { gameConfig.minStakeSenTHV1 } returns List(10) { Int.MAX_VALUE }
        every { heroStakeManager.minStakeHeroConfig } returns emptyMap()
        every { blockRewardDataManager.getRewards(any(), any(), any()) } returns emptyList()
        every { treasureHuntV2Manager.addHeroToPool(any(), any()) } returns emptyList()
        // createRandomMap runs for real; without these it has no live blocks and throws CREATE_MAP_FAIL.
        every { blockDropByDayManager.getBlockDropRate(any(), any()) } returns listOf(100)
        every { blockConfigManager.getConfig(any(), any()) } returns BlockConfig(1, 1, 0)
    }

    fun disableMoveSpeedCheck() {
        every { gameConfig.isCheckPlantMoveSpeed } returns false
    }

    fun addHero(
        id: Int,
        speed: Int = 10,
        bombCount: Int = 1,
        bombRange: Int = 1,
        energy: Int = 100,
        active: Boolean = true,
        stage: Int = GameConstants.BOMBER_STAGE.WORK,
        type: EnumConstants.HeroType = EnumConstants.HeroType.FI,
        abilities: Set<Int> = emptySet(),
    ): Hero {
        val hero: Hero = mockk(relaxed = true)
        val details: IHeroDetails = mockk(relaxed = true)
        every { details.dataType } returns dataType
        every { hero.details } returns details
        every { hero.heroId } returns id
        every { hero.type } returns type
        every { hero.speed } returns speed
        every { hero.bombCount } returns bombCount
        every { hero.bombRange } returns bombRange
        every { hero.energy } returns energy
        every { hero.isActive } returns active
        every { hero.stage } returns stage
        every { hero.hashBombExplode } returns ConcurrentHashMap()
        every { hero.containsAbility(any()) } answers { firstArg<Int>() in abilities }
        every { hero.damageTreasure } returns 1
        every { hero.damageJail } returns 1
        every { hero.totalPower } returns 0
        every { hero.isDangerous } returns false
        every { hero.isHeroS } returns false
        every { hero.rarity } returns 0
        every { hero.stakeBcoin } returns 0.0
        every { hero.stakeSen } returns 0.0
        _heroes[id] = hero
        return hero
    }

    fun removeHero(id: Int) {
        _heroes.remove(id)
    }

    // Encrypts like the Unity client and returns the handler's response.
    fun dispatch(handler: BaseEncryptRequestHandler, request: ISFSObject): ServerResponse {
        val requestId = _nextRequestId++
        val envelope = SFSObject()
        envelope.putInt("rid", requestId)
        envelope.putUtfString(
            "data",
            EncryptionHelper.encrypt(request.toJson(), aesKey),
        )
        val before = outbox.size
        handler.handleClientRequest(_sfsUser, envelope)
        check(outbox.size > before) {
            // No answer usually means the handler threw (see loggedErrors).
            "handler produced no response for request $requestId" +
                if (loggedErrors.isEmpty()) "" else "; server logged: ${loggedErrors.joinToString(" | ")}"
        }
        return outbox.last()
    }

    private fun decodeResponse(command: String, payload: ISFSObject): ServerResponse {
        val requestId = payload.getInt("rid")
        if (payload.containsKey("ec")) {
            return ServerResponse(command, requestId, payload.getInt("ec"), null)
        }
        val encrypted = payload.getByteArray("data")
        val json = EncryptionHelper.decrypt(Base64.getEncoder().encodeToString(encrypted), aesKey)
        return ServerResponse(
            command,
            requestId,
            null,
            SFSObject.newFromJsonData(json),
        )
    }
}

// Plants MainGameExtension.services once per JVM; [bind] switches the current session.
object TestServerRuntime {
    lateinit var globalServices: GlobalServices
        private set
    lateinit var serverServices: ServerServices
        private set

    private var _installed = false
    private var _currentController: IUserController? = null

    fun install() {
        if (_installed) return
        _installed = true

        globalServices = ServiceContainer("test-global")
        serverServices = ServiceContainer("test-server")

        val scheduler: IScheduler = mockk(relaxed = true)
        // Run fireAndForget inline so the response is ready when dispatch() returns.
        every { scheduler.fireAndForget(any()) } answers { firstArg<() -> Unit>().invoke() }

        val usersManager: IUsersManager = mockk(relaxed = true)
        every { usersManager.getUserController(any<User>()) } answers { _currentController }

        val svServices: ISvServicesContainer = mockk(relaxed = true)
        every { svServices.filter(IUsersManager::class) } returns listOf(usersManager)

        val envManager: IEnvManager = mockk(relaxed = true)
        every { envManager.appStage } returns AppStage.TEST

        val coroutineScope: ICoroutineScope = mockk(relaxed = true)

        globalServices.register(IScheduler::class) { scheduler }
        globalServices.register(ISvServicesContainer::class) { svServices }
        globalServices.register(IEnvManager::class) { envManager }
        globalServices.register(ICoroutineScope::class) { coroutineScope }

        plantGlobalServices(globalServices)
    }

    fun bind(controller: IUserController) {
        _currentController = controller
    }

    private fun plantGlobalServices(services: GlobalServices) {
        val field = MainGameExtension::class.java.getDeclaredField("services")
        field.isAccessible = true
        field.set(null, services)
    }
}
