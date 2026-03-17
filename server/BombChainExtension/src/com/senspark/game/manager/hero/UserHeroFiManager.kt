package com.senspark.game.manager.hero

import com.senspark.common.utils.toSFSArray
import com.senspark.game.api.BlockchainHeroResponse
import com.senspark.game.api.IBlockchainDatabaseManager
import com.senspark.game.controller.IUserHouseManager
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.manager.hero.IHeroAbilityConfigManager
import com.senspark.game.data.manager.hero.IHeroBuilder
import com.senspark.game.data.manager.hero.IHeroRepairShieldDataManager
import com.senspark.game.data.manager.treassureHunt.ITreasureHuntConfigManager
import com.senspark.game.data.model.ServerHeroDetails
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.House
import com.senspark.game.data.model.nft.IHeroDetails
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.db.ITHModeDataAccess
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.EnumConstants.*
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.GameConstants
import com.senspark.game.declare.SFSField
import com.senspark.game.declare.customEnum.ChangeRewardReason
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.blockReward.IUserBlockRewardManager
import com.senspark.game.manager.resourceSync.ISyncResourceManager
import com.senspark.game.manager.stake.IHeroStakeManager
import com.senspark.game.utils.Extractor
import com.senspark.game.utils.serialize
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow

class UserHeroFiManager(
    private val _mediator: UserControllerMediator,
    private val _houseManager: IUserHouseManager,
    private val _userBlockRewardManager: IUserBlockRewardManager,
) : IUserHeroFiManager {
    private val _userDataAccess = _mediator.services.get<IUserDataAccess>()
    private val _gameDataAccess = _mediator.services.get<IGameDataAccess>()
    private val _thModeDataAccess = _mediator.services.get<ITHModeDataAccess>()
    private val _heroAbilityConfigManager = _mediator.services.get<IHeroAbilityConfigManager>()
    private val _treasureHuntDataManager = _mediator.services.get<ITreasureHuntConfigManager>()
    private val _gameConfigManager = _mediator.services.get<IGameConfigManager>()

    private val _heroStakeManager = _mediator.svServices.get<IHeroStakeManager>()
    private val _heroSyncService = _mediator.svServices.get<ISyncResourceManager>().heroSyncService
    private val _databaseManager = _mediator.svServices.get<IBlockchainDatabaseManager>()
    private val _heroRepairShieldDataManager = _mediator.svServices.get<IHeroRepairShieldDataManager>()
    private val _heroBuilder = _mediator.svServices.get<IHeroBuilder>()
    private val treasureHuntDataManager = _mediator.services.get<ITreasureHuntConfigManager>()

    private val PAGE_LIMIT = 100

    private val _locker = Any()

    /**
     * Should not use this field directly, use getItems() instead.
     */
    private lateinit var _items: ConcurrentHashMap<Int, Hero>

    override val housingHeroes get() = toArray().filter { it.stage == GameConstants.BOMBER_STAGE.HOUSE }
    override val activeHeroes get() = toArray().filter { it.isActive && it.type != HeroType.TR }
    override val activeHeroCount get() = toArray().count { it.isActive && it.type != HeroType.TR }
    override val trialHeroes get() = toArray().filter { it.type == HeroType.TRIAL }
    override val fiHeroes get() = toArray().filter { it.type == HeroType.FI }
    override val traditionalHeroes get() = toArray().filter { it.type == HeroType.TR }
    override val tonHeroes get() = toArray().filter { it.type == HeroType.TON }
    override val solHeroes get() = toArray().filter { it.type == HeroType.SOL }
    override val ronHeroes get() = toArray().filter { it.type == HeroType.RON }
    override val basHeroes get() = toArray().filter { it.type == HeroType.BAS }
    override val vicHeroes get() = toArray().filter { it.type == HeroType.VIC }


    // Dùng để lưu các hero có sự thay đổi để sync với server khi cần
    private var _listHeroChange: MutableList<Hero> = mutableListOf()

    private fun getItems(): ConcurrentHashMap<Int, Hero> {
        if (!::_items.isInitialized) {
            _items = ConcurrentHashMap()
            loadBomberMan()
        }
        return _items
    }

    private fun loadBomberMan() {
        synchronized(_locker) {
            _items.clear()
            val data = if (_mediator.dataType == DataType.TON) {
                var limit = 0
                if (isIosWeb()) {
                    limit = _gameConfigManager.iosHeroLoaded
                }
                _heroBuilder.getTonHeroes(_mediator.userId, limit)
            } else {
                _heroBuilder.getFiHeroes(_mediator.userId, _mediator.dataType, PAGE_LIMIT, 0)
            }
            val maxActive = _gameConfigManager.maxBomberActive
            var activeCount = 0
            data.forEach {
                if (it.value.isActive) {
                    if (activeCount >= maxActive) {
                        it.value.isActive = false
                        //Nếu deactive hero này thì đảm bảo nó ko ở trong nhà luôn tránh chiếm slot
                        it.value.stage = GameConstants.BOMBER_STAGE.SLEEP
                        _listHeroChange.add(it.value)
                    } else {
                        activeCount += 1
                    }
                }
            }
            //Update lại các hero đã bị thay đổi data với data base cho đồng bộ
            if (_listHeroChange.isNotEmpty()) {
                updateHeroes(_listHeroChange)
            }
            _items.putAll(data)
        }
    }

    override fun loadMoreHeroes(offset: Int, limit: Int) {
        synchronized(_locker) {
            val data = if (_mediator.dataType == DataType.TON) {
                // Ignore for TON for now, as it's handled differently and already has limit.
                // Alternatively, could implement if ton also uses pgsql limits.
                // Assuming it's using pgsql but `getTonHeroes` uses `limit` parameter.
                // We'll just pass limit if we had offset, but getTonHeroes doesn't take offset currently.
                // Keeping as is or returning empty map. To keep it safe, returning empty or not appending.
                emptyMap()
            } else {
                _heroBuilder.getFiHeroes(_mediator.userId, _mediator.dataType, limit, offset)
            }

            val maxActive = _gameConfigManager.maxBomberActive
            var activeCount = activeHeroCount
            data.forEach {
                if (it.value.isActive) {
                    if (activeCount >= maxActive) {
                        it.value.isActive = false
                        it.value.stage = GameConstants.BOMBER_STAGE.SLEEP
                        _listHeroChange.add(it.value)
                    } else {
                        activeCount += 1
                    }
                }
            }
            if (_listHeroChange.isNotEmpty()) {
                updateHeroes(_listHeroChange)
            }
            _items.putAll(data)
        }
    }

    override fun getBombermans(): Map<Int, Hero> {
        return getItems()
    }

    override fun toArray(): List<Hero> {
        return getItems().values.toList()
    }

    override fun hasBomberman(id: Int): Boolean {
        return getItems().containsKey(id)
    }

    override fun getHero(id: Int, heroType: HeroType): Hero? {
        return getItems()[Extractor.parseHeroId(id, heroType)]
    }

    override fun getHero(id: Int, dataType: DataType): Hero? {
        var heroType = HeroType.FI
        if (dataType == DataType.TON) {
            heroType = HeroType.TON
        } else if (dataType == DataType.SOL) {
            heroType = HeroType.SOL
        } else if (dataType == DataType.RON) {
            heroType = HeroType.RON
        } else if (dataType == DataType.BAS) {
            heroType = HeroType.BAS
        } else if (dataType == DataType.VIC) {
            heroType = HeroType.VIC
        }
        return getItems()[Extractor.parseHeroId(id, heroType)]
    }

    override fun getAllHeroTon(): List<Hero> {
        return getItems().values.toList().filter { it.type == HeroType.TON }
    }

    override fun getAllHeroSol(): List<Hero> {
        return getItems().values.toList().filter { it.type == HeroType.SOL }
    }

    override fun getAllHeroFi(): List<Hero> {
        return getItems().values.toList().filter { it.type == HeroType.FI }
    }

    override fun getAllHeroRon(): List<Hero> {
        return getItems().values.toList().filter { it.type == HeroType.RON }
    }

    override fun getAllHeroBas(): List<Hero> {
        return getItems().values.toList().filter { it.type == HeroType.BAS }
    }

    override fun getAllHeroVic(): List<Hero> {
        return getItems().values.toList().filter { it.type == HeroType.VIC }
    }

    override fun addBomberman(hero: Hero) {
        getItems()[Extractor.parseHeroId(hero.heroId, hero.type)] = hero
    }

    override fun removeBomberman(id: Int) {
        if (hasBomberman(id)) {
            getItems().remove(id)
        }
    }

    override fun removeTrialBomberman() {
        trialHeroes.forEach {
            removeBomberman(Extractor.parseHeroId(it.heroId, it.type))
        }
    }

    private fun setHerroStageSleep(bbm: Hero) {
        bbm.stage = GameConstants.BOMBER_STAGE.SLEEP
        bbm.timeRest = System.currentTimeMillis()

        if (!_listHeroChange.contains(bbm)) {
            _listHeroChange.add(bbm)
        }
    }

    /**
     * set hero go sleep và tính lại năng lượng
     *
     * @param bbm            bbm
     * @return energyRecovery
     */
    override fun setSleep(bbm: Hero): Int {
        var energyRecovery = 0
        //neu dang o trong house thi tinh nang luong lai
        if (bbm.stage == GameConstants.BOMBER_STAGE.HOUSE) {
            val uHouse = _houseManager.getHouseHeroRest(bbm)
            val minuteRest = getMinuteRest(bbm)
            energyRecovery = getEnergyIncrease(bbm, minuteRest.toLong(), uHouse)
            energyRecovery = addEnergy(bbm, energyRecovery)
        }
        this.setHerroStageSleep(bbm)
        return energyRecovery
    }

    private fun setHerroStageHouse(bbm: Hero) {
        bbm.stage = GameConstants.BOMBER_STAGE.HOUSE
        bbm.timeRest = System.currentTimeMillis()

        if (!_listHeroChange.contains(bbm)) {
            _listHeroChange.add(bbm)
        }
    }

    override fun setGoHouse(bbm: Hero): Int? {
        //kiem tra có house nao con slot cho hero rest khong
        _houseManager.heroRestInHouse(bbm.heroId) ?: return null
        var energyRecovery = 0
        //nếu hero đang ngủ thì tính năng lượng cho nó truoc khi vào nhà
        if (bbm.stage == GameConstants.BOMBER_STAGE.SLEEP) {
            val minuteRest = getMinuteRest(bbm)
            energyRecovery = getEnergyIncrease(bbm, minuteRest.toLong(), null)
            energyRecovery = addEnergy(bbm, energyRecovery)
        }
        this.setHerroStageHouse(bbm)
        return energyRecovery
    }

    override fun setWork(bbm: Hero): Int {
        var minuteRest = getMinuteRest(bbm)
        var uHouse: House? = null
        if (bbm.stage == GameConstants.BOMBER_STAGE.HOUSE) {
            val houseRent = _houseManager.heroGoWorkFromHouseRent(bbm.heroId)
            if (houseRent != null) {
                uHouse = houseRent
                if (houseRent.endTimeRent < Instant.now().toEpochMilli()) {
                    val maxTimeRest = houseRent.endTimeRent - bbm.timeRest
                    if (minuteRest > maxTimeRest) {
                        minuteRest = maxTimeRest.toInt()
                    }
                }
            } else {
                uHouse = _houseManager.activeHouse
            }
        }
        var energyRecovery = getEnergyIncrease(bbm, minuteRest.toLong(), uHouse)
        // nếu hết energy và energy chưa hồi thì ko cho work nữa
        if (bbm.energy == 0 && energyRecovery == 0) {
            return 0
        }
        energyRecovery = addEnergyAndSetWorking(bbm, energyRecovery)
        return energyRecovery
    }

    override fun getMinuteRest(bbm: Hero): Int {
        //tinh số phút trôi wa
        val current = System.currentTimeMillis()
        val diff = current - bbm.timeRest
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        return minutes.toInt()
    }

    override fun addEnergy(bbm: Hero, energyIncrease: Int): Int {
        return bbm.addEnergy(energyIncrease)
    }

    override fun addEnergyKeepStage(bbm: Hero, energyIncrease: Int): Int {
        bbm.timeRest = System.currentTimeMillis()
        return bbm.addEnergy(energyIncrease)
    }

    override fun addEnergyAndSetWorking(bbm: Hero, energyIncrease: Int): Int {
        bbm.stage = GameConstants.BOMBER_STAGE.WORK
        bbm.timeRest = System.currentTimeMillis()

        if (!_listHeroChange.contains(bbm)) {
            _listHeroChange.add(bbm)
        }
        return bbm.addEnergy(energyIncrease)
    }

    override fun getEnergyIncrease(bbm: Hero, minutes: Long, uHouse: House?): Int {
        //kiem tra xem đang ngủ bụi hay trong nhà để tính năng lượng
        var energyIncreasePerMinute = uHouse?.recovery?.toFloat() ?: 0.5f

        // kiểm tra xem có skill số 5 Fast Charge +5 thể lực/phút đang nghỉ ngơi hay không
        if (bbm.containsAbility(GameConstants.BOMBER_ABILITY.FAST_CHARGE)) {
            energyIncreasePerMinute += _heroAbilityConfigManager.getConfig(GameConstants.BOMBER_ABILITY.FAST_CHARGE).value
        }
        //làm tròn xuống
        val totalEnergyRecovery = floor((energyIncreasePerMinute * minutes).toDouble()).toInt()
        return min(totalEnergyRecovery, bbm.stamina * 50 - bbm.energy)
    }

    // Obsolete: sẽ bỏ trong tương lai
    override fun syncBomberMan(): ISFSObject {
        if (_mediator.dataType.isAirdropUser()) {
            return sendServerHeroToClient(_mediator.dataType)
        }
        if (_mediator.userType != UserType.FI) {
            throw CustomException("User type is not FI", ErrorCode.SERVER_ERROR)
        }
        val dataSync = _databaseManager.heroDatabase.query(_mediator.userId,_mediator.userName, _mediator.dataType)

        val syncHeroData = _heroSyncService.syncHero(_mediator, getItems(), dataSync)
        return sendSyncBomberToClient(syncHeroData.newDetail, syncHeroData.changedHero)
    }

    // Dùng để sync hero từ blockchain với db, đc gọi khi server listen streamkey
    override fun syncHeroAndGetResponse(dataSync: List<BlockchainHeroResponse>): ISFSObject {
        val syncHeroData = _heroSyncService.syncHero(_mediator, getItems(), dataSync)
        return sendSyncBomberToClient(syncHeroData.newDetail, syncHeroData.changedHero)
    }

    // Chỉ gọi thông báo cho api và gửi về số hero đang có trong db
    override fun syncBomberManV3(): ISFSObject {
        if (_mediator.dataType.isAirdropUser()) {
            return sendServerHeroToClient(_mediator.dataType)
        }

        if (_mediator.userType != UserType.FI) {
            throw CustomException("User type is not FI", ErrorCode.SERVER_ERROR)
        }

        val callApiSuccess = _databaseManager.heroDatabase.queryV3(_mediator.userId,_mediator.userName, _mediator.dataType)

        _mediator.logger.log("[SYNC_BOMBERMAN_V3] call api with result = $callApiSuccess")

        val heroesMap = _heroBuilder.getFiHeroes(_mediator.userId, _mediator.dataType, 1000000, 0) // V3 sync gets all or limit logic should be applied? Actually V3 might need to just return what's in cache or all. Let's use getItems() or fetch all if really needed. Keeping max limit for sync.
        val data: ISFSObject = SFSObject()

        val heroes = heroesMap.values.filter { it.type == HeroType.FI }
        val sfsBombers: ISFSArray = SFSArray()

        for (hero in heroes) {
            sfsBombers.addSFSObject(heroToSfsObject(hero))
        }
        data.putSFSArray(SFSField.Bombers, sfsBombers)
        data.putSFSArray(SFSField.NewBombers, SFSArray())
        return data
    }

    private fun sendSyncBomberToClient(
        newDetails: List<IHeroDetails>, changedDetails: List<IHeroDetails>
    ): ISFSObject {
        val data: ISFSObject = SFSObject()
        val sfsBombers: ISFSArray = SFSArray()
        val heroes = this.toArray().filter { it.type == HeroType.FI }
        for (hero in heroes) {
            sfsBombers.addSFSObject(heroToSfsObject(hero))
        }
        val sfsArrNewBBm: ISFSArray = SFSArray()
        for (details in newDetails) {
            sfsArrNewBBm.addLong(details.heroId.toLong())
        }
        for (details in changedDetails) {
            sfsArrNewBBm.addLong(details.heroId.toLong())
        }
        data.putSFSArray(SFSField.Bombers, sfsBombers)
        data.putSFSArray(SFSField.NewBombers, sfsArrNewBBm)
        return data
    }

    override fun sendServerHeroToClient(dataType: DataType): ISFSObject {
        val data: ISFSObject = SFSObject()
        val sfsBombers: ISFSArray = SFSArray()

        val heroes = when (dataType) {
            DataType.TON -> getAllHeroTon()
            DataType.SOL -> getAllHeroSol()
            DataType.RON -> getAllHeroRon()
            DataType.BAS -> getAllHeroBas()
            DataType.VIC -> getAllHeroVic()
            else -> getAllHeroSol()
        }
        for (hero in heroes) {
            sfsBombers.addSFSObject(heroToSfsObject(hero))
        }
        val sfsArrNewBBm: ISFSArray = SFSArray()
        data.putSFSArray(SFSField.Bombers, sfsBombers)
        data.putSFSArray(SFSField.NewBombers, sfsArrNewBBm)
        return data
    }

    private fun heroToSfsObject(hero: Hero): ISFSObject {
        val sfsBomber: ISFSObject = SFSObject()
        sfsBomber.putLong(SFSField.ID, hero.heroId.toLong())
        sfsBomber.putUtfString(SFSField.GenID, hero.details.details)
        sfsBomber.putInt(SFSField.Stage, hero.stage)
        sfsBomber.putInt(SFSField.Energy, hero.energy)
        sfsBomber.putInt(SFSField.Active, if (hero.isActive) 1 else 0)
        sfsBomber.putSFSArray(SFSField.Shields, hero.shield.toSFSArray(hero))
        sfsBomber.putSFSObject("data", hero.toSFSObject())
        sfsBomber.putInt(SFSField.HeroType, hero.type.value)
        sfsBomber.putDouble(SFSField.StakeBcoin, hero.stakeBcoin)
        sfsBomber.putDouble(SFSField.StakeSen, hero.stakeSen)
        //tinh năng lượng đã hồi khi ngủ sau khi đã parse ability
        if (hero.stage != GameConstants.BOMBER_STAGE.WORK) {
            val engeryRecovery = getEnergyRecovery(hero)
            sfsBomber.putInt(SFSField.Restore_HP, engeryRecovery)
        }
        return sfsBomber
    }

    private fun getEnergyRecovery(bbm: Hero): Int {
        val minuteRest = this.getMinuteRest(bbm)
        var uHouse: House? = null
        if (bbm.stage == GameConstants.BOMBER_STAGE.HOUSE) {
            uHouse = _houseManager.getHouseHeroRest(bbm)
        }
        var energyRecovery = this.getEnergyIncrease(bbm, minuteRest.toLong(), uHouse)
        //tinh lai max luon
        val maxEnergy = bbm.stamina * _gameConfigManager.energyMultiplyByStamina
        if (bbm.energy + energyRecovery > maxEnergy) {
            energyRecovery = maxEnergy - bbm.energy
        }
        return energyRecovery
    }

    /**
     * Cập nhật các heroes có details mới vào database
     */
    private fun updateHeroes(listChangedHero: List<Hero>) {
        for (hero in listChangedHero) {
            _gameDataAccess.updateHeroDetails(_mediator.userId, _mediator.dataType, hero)
        }
    }


    override fun repairShield(rewardType: BLOCK_REWARD_TYPE, heroId: Int): ISFSObject {
        val hero = getHero(heroId, HeroType.FI)
            ?: throw CustomException("Hero $heroId not exists", ErrorCode.SERVER_ERROR)
        if (!hero.isHeroS && !hero.isFakeS) {
            throw CustomException("Hero $heroId doesn't have shield")
        }
        val oldFinalDame = hero.shield.items[GameConstants.BOMBER_ABILITY.AVOID_THUNDER] ?: throw CustomException(
            "Hero invalid",
            ErrorCode.SERVER_ERROR
        )

        val config = _heroRepairShieldDataManager.getPrice(hero.rarity, hero.shield.level)
        hero.resetShieldToFull(GameConstants.BOMBER_ABILITY.AVOID_THUNDER)
        try {
            if (rewardType == BLOCK_REWARD_TYPE.ROCK) {
                _userDataAccess.resetShieldHeroWithRock(
                    _mediator.userId,
                    _mediator.dataType.name,
                    hero,
                    oldFinalDame,
                    config.priceRock,
                    rewardType
                )
            } else {
                _userDataAccess.resetShieldHero(
                    _mediator.dataType,
                    _mediator.userId,
                    hero,
                    oldFinalDame,
                    config.price,
                    rewardType
                )
            }

        } catch (ex: Exception) {
            loadBomberMan()
            throw ex
        }
        return heroToSfsObject(hero)
    }

    override fun isBuyHeroesTrial(): Boolean {
        return !(_mediator.userType != UserType.FI || getItems().isNotEmpty() || ((_userBlockRewardManager.get(
            BLOCK_REWARD_TYPE.BCOIN,
        )?.totalValues ?: 0.0) > 0))
    }

    override fun updateStakeAmountHeroes(
        hero: Hero,
        stakeBcoin: Double,
        stakeSen: Double
    ) {
        val detail = hero.details
        val minStakeHeroConfig = _heroStakeManager.minStakeHeroConfig
        // update database
        if (stakeBcoin != hero.stakeBcoin || stakeSen != hero.stakeSen) {
            hero.stakeBcoin = stakeBcoin
            hero.stakeSen = stakeSen
            _gameDataAccess.updateBomberStakeAmount(
                detail.dataType,
                detail.heroId, detail.type.value, hero.stakeBcoin, hero.stakeSen
            )
        }

        if (hero.isHeroS) {
            return
        }

        // không phải hero S thì kiểm tra để thêm shield
        if (stakeBcoin >= minStakeHeroConfig[hero.rarity]!!) {
            val shieldInDatabase = _gameDataAccess.getShieldHeroFromDatabase(
                detail.dataType, hero.heroId,
                hero.type.value
            )
            if (shieldInDatabase == "[]") {
                hero.addBasicShield()
                _gameDataAccess.addShieldToBomber(
                    detail.dataType, hero.heroId,
                    hero.type.value, hero.shield.toString()
                )
            }
        }
    }

    override fun addHeroesServer(detailList: List<ServerHeroDetails>): List<Pair<Int, HeroType>> {
        if (detailList.isEmpty()) {
            return emptyList()
        }

        val newHeroIds = mutableListOf<Pair<Int, HeroType>>()
        var activeHeroes = activeHeroCount
        val maxActive = _gameConfigManager.maxBomberActive

        for (details in detailList) {
            val hero = _heroBuilder.newInstance(_mediator.userId, details)
            if (activeHeroes < maxActive) {
                hero.isActive = true
            }
            val newHeroId = _gameDataAccess.insertNewServerHero(
                _mediator.userName,
                hero,
                _mediator.dataType
            )

            details.setHeroId(newHeroId)
            if (activeHeroes < maxActive) {
                hero.isActive = true
                activeHeroes += 1
            }
            this.addBomberman(hero)
            newHeroIds.add(Pair(newHeroId, hero.type))
        }
        return newHeroIds
    }

    override fun buyHeroServer(quantity: Int, rewardType: BLOCK_REWARD_TYPE): ISFSArray {
        val uid = _mediator.userId
        val dataType = _mediator.dataType

        if (rewardType == BLOCK_REWARD_TYPE.SOL_DEPOSITED ||
            rewardType == BLOCK_REWARD_TYPE.TON_DEPOSITED
        ) {
            if (Instant.now()
                    .toEpochMilli() > treasureHuntDataManager.getTimeDisableBuyWithTokenNetwork(dataType)
            ) {
                throw CustomException("Time buy with $dataType is ended")
            }
        }

        val heroPrice = _treasureHuntDataManager.getPriceHero(dataType)[rewardType]!!
        val reason: String
        val userQuantityHeroes: Int
        when (_mediator.dataType) {
            DataType.TON -> {
                userQuantityHeroes = _gameDataAccess.getUserQuantityHeroes(uid, HeroType.TON, dataType)
                reason = ChangeRewardReason.BUY_HERO_TON
            }

            DataType.RON -> {
                userQuantityHeroes = ronHeroes.size
                reason = ChangeRewardReason.BUY_HERO_RON
            }

            DataType.BAS -> {
                userQuantityHeroes = basHeroes.size
                reason = ChangeRewardReason.BUY_HERO_BAS
            }

            DataType.VIC -> {
                userQuantityHeroes = vicHeroes.size
                reason = ChangeRewardReason.BUY_HERO_VIC
            }

            else -> {
                userQuantityHeroes = getAllHeroSol().size
                reason = ChangeRewardReason.BUY_HERO_SOL
            }
        }
        if (userQuantityHeroes + quantity > _treasureHuntDataManager.getHeroLimit(dataType)) {
            throw CustomException("Heroes get limited")
        }
        var rewardValue = 0f;
        // Mua hero thì dùng COIN network
        if(rewardType == BLOCK_REWARD_TYPE.COIN){
            rewardValue = _userBlockRewardManager.getTotalCoinFiHaving(dataType)
        }
        else{
            rewardValue = _userBlockRewardManager.getRewardValue(rewardType)
        }
        val maxHeroCanBuy = (rewardValue / heroPrice).toInt()
        // tránh việc user cheat gửi số âm hoặc số quá lớn gây lỗi
        if (quantity <= 0 || quantity > maxHeroCanBuy) {
            throw CustomException("Error quantity")
        }
        val price = quantity * heroPrice

        _gameDataAccess.subUserBlockReward(uid, dataType, rewardType, price, reason)

        val tonHeroDetails = mutableListOf<ServerHeroDetails>()
        repeat(quantity) {
            tonHeroDetails.add(ServerHeroDetails.generate(1, dataType))
        }
        val newHeroIds = addHeroesServer(tonHeroDetails)

        // lưu log
        val itemIds = newHeroIds.map { it.first }.toList().serialize()
        _thModeDataAccess.logUserAirdropBuyActivity(uid, itemIds, price, "Buy Hero", dataType)

        val sfsArray = SFSArray()
        for ((heroId, heroType) in newHeroIds) {
            val hero = getHero(heroId, heroType) ?: break
            sfsArray.addSFSObject(heroToSfsObject(hero))
        }
        return sfsArray
    }

    override fun claimHeroServer(quantity: Int): ISFSArray {
        val uid = _mediator.userId
        val dataType = _mediator.dataType
        val reason: String
        val userQuantityHeroes: Int
        when (_mediator.dataType) {
            DataType.TON -> {
                userQuantityHeroes = _gameDataAccess.getUserQuantityHeroes(uid, HeroType.TON, dataType)
                reason = ChangeRewardReason.BUY_HERO_TON
            }

            DataType.RON -> {
                userQuantityHeroes = ronHeroes.size
                reason = ChangeRewardReason.BUY_HERO_RON
            }

            DataType.BAS -> {
                userQuantityHeroes = basHeroes.size
                reason = ChangeRewardReason.BUY_HERO_BAS
            }

            DataType.VIC -> {
                userQuantityHeroes = vicHeroes.size
                reason = ChangeRewardReason.BUY_HERO_VIC
            }

            else -> {
                userQuantityHeroes = getAllHeroSol().size
                reason = ChangeRewardReason.BUY_HERO_SOL
            }
        }
        if (userQuantityHeroes + quantity > _treasureHuntDataManager.getHeroLimit(dataType)) {
            throw CustomException("Heroes get limited")
        }
        _gameDataAccess.subUserBlockReward(uid, dataType, BLOCK_REWARD_TYPE.BOMBERMAN, quantity.toFloat(), reason)
        val tonHeroDetails = mutableListOf<ServerHeroDetails>()
        repeat(quantity) {
            tonHeroDetails.add(ServerHeroDetails.generateWithoutNewRarity(1, dataType))
        }
        val newHeroIds = addHeroesServer(tonHeroDetails)

        // lưu log
        val itemIds = newHeroIds.map { it.first }.toList().serialize()
        _thModeDataAccess.logUserAirdropBuyActivity(uid, itemIds, quantity.toFloat(), "Claim Hero", dataType)

        val sfsArray = SFSArray()
        for ((heroId, heroType) in newHeroIds) {
            val hero = getHero(heroId, heroType) ?: break
            sfsArray.addSFSObject(heroToSfsObject(hero))
        }
        return sfsArray
    }

    override fun fusionHeroServer(
        targetRarity: Int,
        heroList: List<Int>,
        percent: Int,
        priceFusion: Double,
        dataType: DataType
    ): ISFSObject {
        val randomValue = (0..100).random()
        val fusionResult = percent >= randomValue
        var reasonFail = if (!fusionResult) "Fusion fail" else ""

        // Xoá hero và trừ reward
        val rewardType = when (dataType) {
            DataType.RON -> BLOCK_REWARD_TYPE.RON_DEPOSITED
            DataType.BAS -> BLOCK_REWARD_TYPE.BAS_DEPOSITED
            DataType.VIC -> BLOCK_REWARD_TYPE.VIC_DEPOSITED
            else -> BLOCK_REWARD_TYPE.BCOIN_DEPOSITED
        }
        val isFail = _userDataAccess.fusionHeroServer(
            _mediator.userId, heroList, priceFusion, rewardType, dataType
        )

        val response = SFSObject()
        val sfsArray = SFSArray()
        var newHero: Hero? = null
        if (fusionResult && isFail.isEmpty()) {
            val heroesRemoveType = when (dataType) {
                DataType.TON -> HeroType.TON
                DataType.SOL -> HeroType.SOL
                DataType.RON -> HeroType.RON
                DataType.BAS -> HeroType.BAS
                DataType.VIC -> HeroType.VIC
                else -> HeroType.SOL
            }
            heroList.forEach { removeBomberman(Extractor.parseHeroId(it, heroesRemoveType)) }
            // Fusion success, add new hero and log
            val serverHeroDetails = listOf(ServerHeroDetails.generateByRarity(1, dataType, targetRarity))
            val newHeroIds = addHeroesServer(serverHeroDetails)
            val newHeroId = newHeroIds.first().first
            _thModeDataAccess.logUserFusionHeroServer(
                _mediator.userId, heroList, newHeroId.toString(), reasonFail, priceFusion, dataType
            )
            for ((heroId, heroType) in newHeroIds) {
                newHero = getHero(heroId, heroType) ?: break
                sfsArray.addLong(heroId.toLong())
            }
        } else {
            // Fusion fail, log the failure reason
            if (isFail.isNotEmpty()) reasonFail = isFail
            _thModeDataAccess.logUserFusionHeroServer(
                _mediator.userId, heroList, "", reasonFail, priceFusion, dataType
            )
        }
        //Cập nhật các trạng thái của những hero đã thay đổi vào data base trước khi fusion
        syncHeroServerWithDataBase(heroList)
        //Load lại hero từ data base để đảm bảo client ko bị thiếu hero
        loadBomberMan()

        // Đảm bảo hero mới fusion ra sẽ đc trả về dù có nằm ngoài limit trả về
        if (fusionResult && isFail.isEmpty()) {
            val newHeroId = sfsArray.getLong(0).toInt()
            if (!hasBomberman(newHeroId)) {
                if (newHero != null)
                    addBomberman(newHero)
            }
        }

        val allHeroes = when (dataType) {
            DataType.TON -> getAllHeroTon()
            DataType.RON -> getAllHeroRon()
            DataType.BAS -> getAllHeroBas()
            DataType.VIC -> getAllHeroVic()
            else -> getAllHeroSol()
        }
        response.putBool("result", sfsArray.size() > 0)
        response.putSFSArray(SFSField.NewBombers, sfsArray)
        response.putSFSArray(SFSField.Bombers, allHeroes.toSFSArray { it.toSFSObject() })

        return response
    }

    override fun multiFusionHeroServer(targetRarity: Int, heroList: List<Int>, rarity: Int): ISFSObject {
        val dataType = _mediator.dataType

        // lấy ra danh sách rarity sau khi fusion sẽ nhận được
        val mapHeroFusion = getQuantityMultiFusion(heroList.size, rarity, targetRarity)
        if (mapHeroFusion.containsKey(rarity)) {
            throw Exception("Wrong quantity heroes")
        }
        val priceFusion = getPriceMultiFusion(mapHeroFusion, rarity)

        // Lấy ra danh sách rarity sau khi fusion sẽ nhận được
        val mapHeroResult = getResultMultiFusion(heroList, rarity, targetRarity)

        // xoá hero trong database và trừ reward
        val rewardType = when (dataType) {
            DataType.RON -> BLOCK_REWARD_TYPE.RON_DEPOSITED
            DataType.BAS -> BLOCK_REWARD_TYPE.BAS_DEPOSITED
            DataType.VIC -> BLOCK_REWARD_TYPE.VIC_DEPOSITED
            else -> BLOCK_REWARD_TYPE.BCOIN_DEPOSITED
        }
        val isFail = _userDataAccess.fusionHeroServer(
            _mediator.userId, heroList, priceFusion, rewardType, dataType
        )
        if (isFail.isNotEmpty()) {
            throw Exception(isFail)
        }

        // xoá heroes trên server
        val heroesRemoveType = when (dataType) {
            DataType.TON -> HeroType.TON
            DataType.SOL -> HeroType.SOL
            DataType.RON -> HeroType.RON
            DataType.BAS -> HeroType.BAS
            DataType.VIC -> HeroType.VIC
            else -> HeroType.SOL
        }
        heroList.forEach { removeBomberman(Extractor.parseHeroId(it, heroesRemoveType)) }

        // thêm hero mới
        val serverHeroDetails = mutableListOf<ServerHeroDetails>()
        mapHeroResult.forEach { (skin, rarityMap) ->
            rarityMap.forEach { (rarity, quantity) ->
                repeat(quantity) {
                    val newHeroDetail = ServerHeroDetails.generateByRarity(1, dataType, rarity)
                    // chỉ tạo skin dưới mega (rarity 5)
                    if (skin != -1 && rarity <= 5) {
                        newHeroDetail.color = _gameConfigManager.heroSpecialColor
                        newHeroDetail.skin = skin
                    }
                    serverHeroDetails.add(newHeroDetail)
                }
            }
        }
        // thêm hero mới trên server và database
        val newHeroIds = addHeroesServer(serverHeroDetails)

        // lưu log
        val newHeroIdsString = serverHeroDetails.joinToString(",") { it.heroId.toString() }
        _thModeDataAccess.logUserFusionHeroServer(
            _mediator.userId, heroList, newHeroIdsString, "", priceFusion, dataType
        )

        //Cập nhật các trạng thái của những hero đã thay đổi vào data base trước khi fusion
        syncHeroServerWithDataBase(heroList)
        //Load lại hero từ data base để đảm bảo client ko bị thiếu hero
        loadBomberMan()

        val allHeroes = when (dataType) {
            DataType.TON -> getAllHeroTon()
            DataType.RON -> getAllHeroRon()
            DataType.BAS -> getAllHeroBas()
            DataType.VIC -> getAllHeroVic()
            else -> getAllHeroSol()
        }
        val response = SFSObject()
        response.putBool("result", newHeroIds.isNotEmpty())
        response.putSFSArray(SFSField.NewBombers, newHeroIds.toSFSArray { it.first })
        response.putSFSArray(SFSField.Bombers, allHeroes.toSFSArray { it.toSFSObject() })
        return response
    }

    // hàm này tính toán những hero nhận được khi multi fusion
    // output: Map<skin, Map<rarity, quantity>>
    private fun getResultMultiFusion(
        heroList: List<Int>,
        startRarity: Int,
        endRarity: Int,
        base: Int = 4
    ): MutableMap<Int, MutableMap<Int, Int>> {
        val dataType = _mediator.dataType
        val result: MutableMap<Int, MutableMap<Int, Int>> = mutableMapOf()
        for (hero in heroList) {
            val heroData = getHero(hero, dataType)
            if (heroData != null) {
                val heroMap = result.getOrPut(heroData.skin) { mutableMapOf() }
                heroMap[heroData.rarity] = heroMap.getOrDefault(heroData.rarity, 0) + 1
            }
        }
        val randomSkin = result.getOrPut(-1) { mutableMapOf() }
        randomSkin[startRarity] = randomSkin.getOrDefault(startRarity, 0)

        for (rarityStep in startRarity..<endRarity) {
            var remainCount = 0
            for ((skin, rarityMap) in result) {
                // bỏ qua list random
                if (skin == -1) continue
                // tính số heroes sau khi fusion còn giữ skin
                val count = rarityMap[rarityStep] ?: 0
                val fusedHeroes = count.div(base)
                val remainHeroes = count.rem(base)
                val skinMap = result.getOrPut(skin) { mutableMapOf() }
                skinMap[rarityStep + 1] = skinMap.getOrDefault(rarityStep + 1, 0) + fusedHeroes
                skinMap[rarityStep] = 0
                remainCount += remainHeroes
            }
            // tính số heroes sau khi fusion không còn skin, vào list random
            val randomCount = remainCount + (randomSkin[rarityStep] ?: 0)
            val randomFused = randomCount.div(base)
            val randomRemain = randomCount.rem(base)
            randomSkin[rarityStep] = randomRemain
            randomSkin[rarityStep + 1] = randomSkin.getOrDefault(rarityStep + 1, 0) + randomFused
        }

        // kiểm tra lại result, nếu số lượng là 0 thì xoá
        val iterator = result.iterator()
        while (iterator.hasNext()) {
            val (skin, rarityMap) = iterator.next()
            rarityMap.entries.removeIf { it.value == 0 }
            if (rarityMap.isEmpty()) {
                iterator.remove()
            }
        }
        return result
    }

    // hàm này tính toán những hero nhận được khi multi fusion
    // output: Map<rarity, quantity>
    private fun getQuantityMultiFusion(
        numberHeroes: Int,
        startRarity: Int,
        endRarity: Int,
        base: Int = 4
    ): Map<Int, Int> {
        var n = numberHeroes
        val result = mutableMapOf<Int, Int>()

        var power = 0
        var rarity = startRarity
        while (n > 0) {
            if (rarity == endRarity) {
                result[endRarity] = n
                break
            }
            val remainder = n % base
            if (remainder > 0) {
                result[rarity] = remainder
            }
            n /= base
            power++
            rarity++
        }

        return result
    }

    private fun getPriceMultiFusion(listFusion: Map<Int, Int>, startRarity: Int): Double {
        var price = 0.0
        val fusionFeeConfig = _treasureHuntDataManager.getFusionFeeConfig(_mediator.dataType)
        listFusion.forEach {
            var nextRarity = startRarity + 1
            while (nextRarity <= it.key) {
                price += (fusionFeeConfig[nextRarity] * it.value) * 4f.pow(it.key - nextRarity)
                nextRarity += 1
            }
        }
        return price
    }

    private fun syncHeroServerWithDataBase(heroNeedDelete: List<Int>) {
        if (!_mediator.isAirdropUser()) {
            throw CustomException("User type is not airdrop", ErrorCode.SERVER_ERROR)
        }
        //Ko có hero nào cần update
        if (_listHeroChange.isEmpty()) {
            return
        }
        //Xoá các hero đã bị delete khỏi danh sách để ko cập nhật lại trạng thái
        _listHeroChange.removeAll { hero -> heroNeedDelete.contains(hero.heroId) }
        //Update các hero có thay đổi vào database
        updateHeroes(_listHeroChange)
        _listHeroChange.clear()
    }

    private fun isIosWeb(): Boolean {
        return _mediator.platform != null && _mediator.platform == Platform.IosTelegram
    }
}
