package com.senspark.client.treasure

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
import com.senspark.game.data.model.user.BlockMap
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
import com.senspark.game.handler.airdropUser.StartTreasureModeHandler
import com.senspark.game.handler.sol.EncryptionHelper
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IMasterUserManager
import com.senspark.game.manager.IUsersManager
import com.senspark.game.manager.blockMap.UserBlockMapManagerV2
import com.senspark.game.manager.blockMap.mapservice.IMapServiceClient
import com.senspark.game.manager.blockMap.mapservice.IMapTreasureEventRouter
import com.senspark.game.manager.blockMap.mapservice.MapTreasureEventRouter
import com.senspark.game.manager.blockMap.mapservice.MsAutoHeroesRequest
import com.senspark.game.manager.blockMap.mapservice.MsAutoKeepaliveResponse
import com.senspark.game.manager.blockMap.mapservice.MsAutoSnapshotDto
import com.senspark.game.manager.blockMap.mapservice.MsAutoStartRequest
import com.senspark.game.manager.blockMap.mapservice.MsBlockDto
import com.senspark.game.manager.blockMap.mapservice.MsHeroPositionDto
import com.senspark.game.manager.blockMap.mapservice.MsMapInitRequest
import com.senspark.game.manager.blockMap.mapservice.MsMapReplaceRequest
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

class FakeMapServiceClient : IMapServiceClient {
    val replacedMaps = mutableListOf<MsMapReplaceRequest>()

    // Records calls; the snapshot echoes the last map MapService was given.
    val autoStarts = mutableListOf<MsAutoStartRequest>()
    val autoHeroEdits = mutableListOf<MsAutoHeroesRequest>()
    var autoStops = 0
    val autoPauses = mutableListOf<Boolean>()
    var autoRunning = false
    var autoPaused = false
    var autoSeq = 0L
    var awaitingNewMap = false
    var keepaliveError: Exception? = null
    private var _blocks: List<MsBlockDto> = emptyList()

    override fun initSession(sessionKey: String, request: MsMapInitRequest) {
        _blocks = request.blocks
    }

    override fun replaceMap(sessionKey: String, request: MsMapReplaceRequest) {
        replacedMaps.add(request)
        _blocks = request.blocks
    }

    override fun autoStart(sessionKey: String, request: MsAutoStartRequest): MsAutoSnapshotDto {
        autoStarts.add(request)
        autoRunning = true
        autoPaused = request.paused
        return MsAutoSnapshotDto(
            seq = autoSeq,
            serverTimeMs = 1_000,
            fuseMs = request.fuseMs ?: 3000,
            heroes = request.heroes.mapIndexed { k, h -> MsHeroPositionDto(h.hero.heroId, 0, 2 * k) },
            blocks = _blocks,
            awaitingNewMap = awaitingNewMap,
        )
    }

    override fun autoHeroes(sessionKey: String, request: MsAutoHeroesRequest): Boolean {
        if (!autoRunning) return false
        autoHeroEdits.add(request)
        return true
    }

    override fun autoPause(sessionKey: String, paused: Boolean): Boolean {
        if (!autoRunning) return false
        autoPauses.add(paused)
        autoPaused = paused
        return true
    }

    override fun autoStop(sessionKey: String) {
        autoStops++
        autoRunning = false
    }

    override fun autoKeepalive(sessionKey: String): MsAutoKeepaliveResponse {
        keepaliveError?.let { throw it }
        return MsAutoKeepaliveResponse(autoRunning, autoSeq, autoPaused)
    }

    override fun deleteSession(sessionKey: String) {}
}

// Real UserBlockMapManagerV2 + StartTreasureModeHandler + router (inline); MapService is [mapService].
class TreasureTestBed(
    val map: MapData,
    val mapService: IMapServiceClient = FakeMapServiceClient(),
    val userId: Int = 1,
    val dataType: DataType = DataType.BSC,
    treasureRouter: IMapTreasureEventRouter? = null,
) {
    data class Push(val command: String, val data: ISFSObject)

    val pushes = mutableListOf<Push>().let { java.util.Collections.synchronizedList(it) }
    val saves = mutableListOf<EnumConstants.SAVE>().let { java.util.Collections.synchronizedList(it) }
    val loggedErrors = mutableListOf<String>().let { java.util.Collections.synchronizedList(it) }

    val gameConfig: IGameConfigManager = mockk(relaxed = true)
    val gameDataAccess: IGameDataAccess = mockk(relaxed = true)
    val treasureHuntV2Manager: ITreasureHuntV2Manager = mockk(relaxed = true)
    val heroStakeManager: IHeroStakeManager = mockk(relaxed = true)
    val blockRewardDataManager: IBlockRewardDataManager = mockk(relaxed = true)
    val blockRewardManager: IUserBlockRewardManager = mockk(relaxed = true)
    val blockDropByDayManager: IBlockDropByDayManager = mockk(relaxed = true)
    val blockConfigManager: IBlockConfigManager = mockk(relaxed = true)
    val scheduler: IScheduler = mockk(relaxed = true)
    val envManager: IEnvManager = mockk(relaxed = true)
    val logger: IServerLogger = mockk(relaxed = true)
    val controller: IUserController = mockk(relaxed = true)
    val heroFiManager: IUserHeroFiManager = mockk(relaxed = true)
    val masterUserManager: IMasterUserManager = mockk(relaxed = true)

    val treasureRouter: IMapTreasureEventRouter = treasureRouter ?: MapTreasureEventRouter(logger) { java.util.concurrent.Executor { it.run() } }
    val blockMap: UserBlockMapManagerV2
    val startTreasureModeHandler: StartTreasureModeHandler
    val sessionKey get() = "$userId-$dataType-PVE_V2"

    private val _heroes = ConcurrentHashMap<Int, Hero>()
    private val _aesKey: SecretKey = AesEncryption.generateKey()
    private val _responses = mutableListOf<ISFSObject>()
    private val _sfsUser: User = mockk(relaxed = true)
    private var _nextRequestId = 1

    init {
        TestServerRuntime.install()

        every { gameConfig.timeBombExplode } returns 3000
        every { gameConfig.blockDensity } returns 0.5f
        every { gameConfig.maxTitleset } returns 1
        every { heroStakeManager.minStakeHeroConfig } returns emptyMap()
        every { treasureHuntV2Manager.addHeroToPool(any(), any()) } returns emptyList()
        every { blockDropByDayManager.getBlockDropRate(any(), any()) } returns listOf(100)
        every { blockConfigManager.getConfig(any(), any()) } returns BlockConfig(1, 1, 0)

        val globalServices: GlobalServices = ServiceContainer("treasure-bed-global")
        val serverServices: ServerServices = ServiceContainer("treasure-bed-server")
        val dataAccessManager: IDataAccessManager = mockk(relaxed = true)
        every { dataAccessManager.gameDataAccess } returns gameDataAccess
        globalServices.register(IBlockConfigManager::class) { blockConfigManager }
        globalServices.register(IDataAccessManager::class) { dataAccessManager }
        globalServices.register(IBlockDropByDayManager::class) { blockDropByDayManager }
        globalServices.register(IBlockRewardDataManager::class) { blockRewardDataManager }
        globalServices.register(IGameConfigManager::class) { gameConfig }
        globalServices.register(IScheduler::class) { scheduler }
        // Relaxed mock: treasureEventsBatchMs = 0, so pushes are immediate unless a test sets it.
        globalServices.register(IEnvManager::class) { envManager }
        val coroutineScope: ICoroutineScope = mockk(relaxed = true)
        every { coroutineScope.scope } returns kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        globalServices.register(ICoroutineScope::class) { coroutineScope }
        globalServices.register(IMapTreasureEventRouter::class) { this.treasureRouter }
        serverServices.register(ITreasureHuntV2Manager::class) { treasureHuntV2Manager }
        serverServices.register(IHeroStakeManager::class) { heroStakeManager }

        val mediator = UserControllerMediator(
            userId = userId,
            dataType = dataType,
            userName = "treasure-test-$userId",
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
            tryToKickAndWriteLogHack = { _, _ -> false },
            setUsedPvpBoosterToDatabase = { },
            saveGameAndLoadReward = { },
            isCheatByMultipleLogin = { false },
            sendDataEncryption = { cmd, data, _ -> pushes.add(Push(cmd, data)) },
        )
        every { gameDataAccess.loadSingleMapData(any(), any(), any()) } returns map

        blockMap = UserBlockMapManagerV2(mediator, blockRewardManager, heroFiManager, mapService)

        every { masterUserManager.userBlockMapManagerV2 } returns blockMap
        every { masterUserManager.heroFiManager } returns heroFiManager
        every { heroFiManager.getHero(any<Int>(), any<DataType>()) } answers { _heroes[firstArg()] }
        every { heroFiManager.activeHeroes } answers { _heroes.values.filter { it.isActive } }
        every { controller.checkHash() } returns true
        every { controller.svServices } returns serverServices
        val legacyBlockMap: com.senspark.game.manager.blockMap.IUserBlockMapManager = mockk(relaxed = true)
        every { legacyBlockMap.getBombermanDangerous(any()) } returns SFSObject().apply { putSFSArray("dangerous", com.smartfoxserver.v2.entities.data.SFSArray()) }
        every { masterUserManager.userBlockMapManager } returns legacyBlockMap

        val userInfo: IUserInfo = mockk(relaxed = true)
        every { userInfo.aesKey } returns _aesKey
        every { userInfo.type } returns EnumConstants.UserType.FI
        every { controller.userInfo } returns userInfo
        every { controller.userId } returns userId
        every { controller.userName } returns "treasure-test-$userId"
        every { controller.dataType } returns dataType
        every { controller.landing } returns EnumConstants.Landing.TREASURE
        every { controller.logger } returns logger
        every { controller.masterUserManager } returns masterUserManager
        every { controller.isInitialized() } returns true
        every { controller.user } returns null

        every { logger.error(any<String>()) } answers { loggedErrors.add(firstArg()); Unit }
        every { logger.error(any<String>(), any<Exception>()) } answers {
            loggedErrors.add("${firstArg<String>()}: ${secondArg<Exception>()}"); Unit
        }

        val payload = slot<ISFSObject>()
        every { controller.send(any(), capture(payload), any()) } answers { _responses.add(payload.captured) }

        TestServerRuntime.bind(controller)
        startTreasureModeHandler = StartTreasureModeHandler()
    }

    fun addHero(id: Int, energy: Int = 100, active: Boolean = true, stage: Int = GameConstants.BOMBER_STAGE.WORK): Hero {
        val hero: Hero = mockk(relaxed = true)
        val details: IHeroDetails = mockk(relaxed = true)
        every { details.dataType } returns dataType
        every { hero.details } returns details
        every { hero.heroId } returns id
        every { hero.type } returns EnumConstants.HeroType.FI
        every { hero.speed } returns 10
        every { hero.bombCount } returns 1
        every { hero.bombRange } returns 2
        every { hero.energy } returns energy
        every { hero.isActive } returns active
        every { hero.stage } returns stage
        every { hero.containsAbility(any()) } returns false
        every { hero.damageTreasure } returns 7
        every { hero.damageJail } returns 5
        every { hero.totalPower } returns 1
        every { hero.isDangerous } returns false
        every { hero.rarity } returns 2
        _heroes[id] = hero
        return hero
    }

    fun startTreasureViaHandler(): ISFSObject? = callHandler(startTreasureModeHandler, SFSObject())

    // Full encrypted round trip, like a live request.
    fun callHandler(handler: com.senspark.game.handler.sol.BaseEncryptRequestHandler, request: ISFSObject): ISFSObject? {
        val envelope = SFSObject()
        envelope.putInt("rid", _nextRequestId++)
        envelope.putUtfString("data", EncryptionHelper.encrypt(request.toJson(), _aesKey))
        val before = _responses.size
        handler.handleClientRequest(_sfsUser, envelope)
        check(_responses.size > before) { "handler produced no response; logged: $loggedErrors" }
        val response = _responses.last()
        if (response.containsKey("ec")) return null
        val json = EncryptionHelper.decrypt(Base64.getEncoder().encodeToString(response.getByteArray("data")), _aesKey)
        return SFSObject.newFromJsonData(json)
    }

    fun pushesOf(command: String) = synchronized(pushes) { pushes.filter { it.command == command } }

    companion object {
        fun mapOf(vararg cells: Pair<Int, Int>, hp: Int = 1, type: Int = GameConstants.BLOCK_TYPE.WOODEN): MapData {
            val map = MapData()
            map.addBlocks(cells.map { (i, j) ->
                BlockMap().apply {
                    this.i = i
                    this.j = j
                    this.type = type
                    this.hp = hp
                    this.maxHp = hp
                }
            })
            return map
        }
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
