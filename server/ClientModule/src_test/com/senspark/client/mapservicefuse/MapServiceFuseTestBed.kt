package com.senspark.client.mapservicefuse

import com.senspark.client.explodev6.TestServerRuntime
import com.senspark.common.service.IScheduler
import com.senspark.common.service.ServiceContainer
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
import com.senspark.game.extension.ServerServices
import com.senspark.game.extension.coroutines.ICoroutineScope
import com.senspark.game.handler.airdropUser.StartPlantBombHandler
import com.senspark.game.handler.sol.EncryptionHelper
import com.senspark.game.manager.IMasterUserManager
import com.senspark.game.manager.blockMap.UserBlockMapManagerV2
import com.senspark.game.manager.blockMap.mapservice.IMapExplodeResultRouter
import com.senspark.game.manager.blockMap.mapservice.IMapServiceClient
import com.senspark.game.manager.blockMap.mapservice.MapExplodeResultRouter
import com.senspark.game.manager.blockMap.mapservice.MsCellDto
import com.senspark.game.manager.blockMap.mapservice.MsExplodeRequest
import com.senspark.game.manager.blockMap.mapservice.MsExplodeResponse
import com.senspark.game.manager.blockMap.mapservice.MsMapInitRequest
import com.senspark.game.manager.blockMap.mapservice.MsMapReplaceRequest
import com.senspark.game.manager.blockMap.mapservice.MsPlantRequest
import com.senspark.game.manager.blockMap.mapservice.MsPlantResponse
import com.senspark.game.manager.blockMap.mapservice.MsTargetsRequest
import com.senspark.game.manager.blockMap.mapservice.MsTargetsResponse
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
    val plants = mutableListOf<MsPlantRequest>()
    val replacedMaps = mutableListOf<MsMapReplaceRequest>()
    var explodeCalls = 0
    var plantResponse = MsPlantResponse(result = "OK", nextTarget = MsCellDto(2, 0), fuseArmed = true)

    override fun initSession(sessionKey: String, request: MsMapInitRequest) {}
    override fun replaceMap(sessionKey: String, request: MsMapReplaceRequest) {
        replacedMaps.add(request)
    }

    override fun deleteSession(sessionKey: String) {}
    override fun getTargets(sessionKey: String, request: MsTargetsRequest) = MsTargetsResponse()
    override fun plantBomb(sessionKey: String, request: MsPlantRequest): MsPlantResponse {
        plants.add(request)
        return plantResponse
    }

    override fun explode(sessionKey: String, request: MsExplodeRequest): MsExplodeResponse {
        explodeCalls++
        return MsExplodeResponse("ALREADY_TAKEN")
    }
}

// Real UserBlockMapManagerV2 + StartPlantBombHandler + router (inline); MapService is [mapService].
class MapServiceFuseTestBed(
    val map: MapData,
    val mapService: IMapServiceClient = FakeMapServiceClient(),
    val userId: Int = 1,
    val dataType: DataType = DataType.BSC,
    router: IMapExplodeResultRouter? = null,
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
    val logger: IServerLogger = mockk(relaxed = true)
    val controller: IUserController = mockk(relaxed = true)
    val heroFiManager: IUserHeroFiManager = mockk(relaxed = true)
    val masterUserManager: IMasterUserManager = mockk(relaxed = true)

    val router: IMapExplodeResultRouter = router ?: MapExplodeResultRouter(logger) { java.util.concurrent.Executor { it.run() } }
    val blockMap: UserBlockMapManagerV2
    val startPlantBombHandler: StartPlantBombHandler
    val sessionKey get() = "$userId-$dataType-PVE_V2"

    private val _heroes = ConcurrentHashMap<Int, Hero>()
    private val _aesKey: SecretKey = AesEncryption.generateKey()
    private val _responses = mutableListOf<ISFSObject>()
    private val _sfsUser: User = mockk(relaxed = true)
    private var _nextRequestId = 1

    init {
        TestServerRuntime.install()

        every { gameConfig.isCheckPlantMoveSpeed } returns false
        every { gameConfig.timeBombExplode } returns 3000
        every { gameConfig.blockDensity } returns 0.5f
        every { gameConfig.maxTitleset } returns 1
        every { heroStakeManager.minStakeHeroConfig } returns emptyMap()
        every { treasureHuntV2Manager.addHeroToPool(any(), any()) } returns emptyList()
        every { blockDropByDayManager.getBlockDropRate(any(), any()) } returns listOf(100)
        every { blockConfigManager.getConfig(any(), any()) } returns BlockConfig(1, 1, 0)

        val globalServices: GlobalServices = ServiceContainer("fuse-bed-global")
        val serverServices: ServerServices = ServiceContainer("fuse-bed-server")
        val dataAccessManager: IDataAccessManager = mockk(relaxed = true)
        every { dataAccessManager.gameDataAccess } returns gameDataAccess
        globalServices.register(IBlockConfigManager::class) { blockConfigManager }
        globalServices.register(IDataAccessManager::class) { dataAccessManager }
        globalServices.register(IBlockDropByDayManager::class) { blockDropByDayManager }
        globalServices.register(IBlockRewardDataManager::class) { blockRewardDataManager }
        globalServices.register(IGameConfigManager::class) { gameConfig }
        globalServices.register(IScheduler::class) { scheduler }
        val coroutineScope: ICoroutineScope = mockk(relaxed = true)
        every { coroutineScope.scope } returns kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        globalServices.register(ICoroutineScope::class) { coroutineScope }
        globalServices.register(IMapExplodeResultRouter::class) { this.router }
        serverServices.register(ITreasureHuntV2Manager::class) { treasureHuntV2Manager }
        serverServices.register(IHeroStakeManager::class) { heroStakeManager }

        val mediator = UserControllerMediator(
            userId = userId,
            dataType = dataType,
            userName = "map-fuse-test-$userId",
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

        val userInfo: IUserInfo = mockk(relaxed = true)
        every { userInfo.aesKey } returns _aesKey
        every { controller.userInfo } returns userInfo
        every { controller.userId } returns userId
        every { controller.userName } returns "map-fuse-test-$userId"
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
        startPlantBombHandler = StartPlantBombHandler()
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

    // Returns the decrypted response, or null on error.
    fun plantViaHandler(heroId: Int, bombNo: Int, i: Int, j: Int): ISFSObject? {
        val request = SFSObject()
        request.putInt("id", heroId)
        request.putInt("num", bombNo)
        request.putInt("i", i)
        request.putInt("j", j)
        val envelope = SFSObject()
        envelope.putInt("rid", _nextRequestId++)
        envelope.putUtfString("data", EncryptionHelper.encrypt(request.toJson(), _aesKey))
        val before = _responses.size
        startPlantBombHandler.handleClientRequest(_sfsUser, envelope)
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
