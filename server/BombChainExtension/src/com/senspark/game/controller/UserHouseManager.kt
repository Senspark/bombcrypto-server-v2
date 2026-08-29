package com.senspark.game.controller

import com.senspark.common.utils.toSFSArray
import com.senspark.game.data.manager.treassureHunt.IHouseManager
import com.senspark.game.data.manager.treassureHunt.ITreasureHuntConfigManager
import com.senspark.game.data.model.config.HouseRentPackage
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.House
import com.senspark.game.data.model.nft.HouseDetails
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.db.ITHModeDataAccess
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.GameConstants
import com.senspark.game.declare.customEnum.ChangeRewardReason
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.hero.IUserHeroFiManager
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

class UserHouseManager(
    mediator: UserControllerMediator,
) : IUserHouseManager {

    private val gameDataAccess: IGameDataAccess = mediator.services.get()
    private val thModeDataAccess: ITHModeDataAccess = mediator.services.get()
    private val treasureHuntDataManager: ITreasureHuntConfigManager = mediator.services.get()
	private val houseManager: IHouseManager = mediator.svServices.get()

    /**
     * WARNING: should not use directly, use getUserHouses() instead
     */
    private lateinit var _items: ConcurrentHashMap<Int, House>
    private lateinit var heroManager: IUserHeroFiManager
    private val _dataType = mediator.dataType
    private val _userId = mediator.userId

    private val _heroHouseRent: ConcurrentHashMap<Int, Int> =
        ConcurrentHashMap(gameDataAccess.loadHeroInHouseRent(_dataType, _userId))
    private val _packageHouseRent: List<HouseRentPackage> = houseManager.listConfigPackages[_dataType] ?: listOf()

    // TODO: Restore PAGE_LIMIT = 100 once the SFS command handler for SYNC_MORE_HOUSE is implemented
    //       on the server side, so that loadMoreHouses() is actually reachable from the client.
    //       Ref: client PR bombcrypto-client-v2#5
    private val PAGE_LIMIT = Int.MAX_VALUE

    private fun getItems(): MutableMap<Int, House> {
        if (!::_items.isInitialized) {
            val mapHouse = gameDataAccess.loadUserHouse(_dataType, _userId, PAGE_LIMIT, 0)
            _items = ConcurrentHashMap(mapHouse)
        }
        return _items
    }

    override fun loadMoreHouses(offset: Int, limit: Int) {
        val newHouses = gameDataAccess.loadUserHouse(_dataType, _userId, limit, offset)
        if (::_items.isInitialized) {
            _items.putAll(newHouses)
        } else {
            _items = ConcurrentHashMap(newHouses)
        }
    }

    override fun initHeroManager(heroManager: IUserHeroFiManager) {
        this.heroManager = heroManager
    }

    override val activeHouse: House?
        get() {
            val houses = getItems().values
            val active = houses.firstOrNull { it.isActive }
            if (active == null && houses.isNotEmpty()) {
                val first = houses.first()
                first.isActive = true
                gameDataAccess.updateUserHouseStage(_dataType, _userId, listOf(first))
                return first
            }
            return active
        }

    override fun getHouse(id: Int): House? {
        return getItems()[id]
    }

    override fun toArray(): List<House> {
        return getItems().values.toList()
    }

    override fun setActiveHouse(houseId: Int) {
        val oldActiveHouse = activeHouse
        val newActiveHouse = getHouse(houseId)
        
        if (newActiveHouse != null) {
            val updateList = mutableListOf<House>()
            
            // Deactivate old house if it's different
            if (oldActiveHouse != null && oldActiveHouse.houseId != newActiveHouse.houseId) {
                oldActiveHouse.isActive = false
                updateList.add(oldActiveHouse)
            }
            
            // Activate new house
            newActiveHouse.isActive = true
            updateList.add(newActiveHouse)
            
            // Save to database
            gameDataAccess.updateUserHouseStage(_dataType, _userId, updateList)
        }
    }

    override fun addHouse(uHouse: House) {
        getItems()[uHouse.houseId] = uHouse
    }

    override fun hasHouse(houseId: Int): Boolean {
        return getItems().containsKey(houseId)
    }

    override fun getUserHouses(): Map<Int, House> {
        return getItems()
    }

    override fun removeHouse(houseId: Int) {
        if (hasHouse(houseId)) {
            getItems().remove(houseId)
        }
    }

    override fun changeActiveHouse(
        newHouse: House,
        oldHouse: House,
    ): List<Hero> {
        val arr: MutableList<House> = ArrayList()
        arr.add(oldHouse)
        arr.add(newHouse)
        val currBbmInHouse = heroManager.housingHeroes.size
        newHouse.isActive = true
        oldHouse.isActive = false

        //TH khong có bomberman nao dang trong house thi cap nhat lai active house là xong
        //Hoặc là active cùng loại nhà. như nhà common sang nhà common khác
        if (currBbmInHouse <= 0 || newHouse.houseId == oldHouse.houseId) {
            gameDataAccess.updateUserHouseStage(_dataType, _userId, arr)
            return ArrayList()
        }

        //lấy danh sách con bomber đang trong house ra
        val lstBbm = heroManager.housingHeroes
        val energiesRecovery: MutableMap<Int, Int> = HashMap()
        //tính lại năng lượng cho nó
        for (bbm in lstBbm) {
            val minuteRest = heroManager.getMinuteRest(bbm)
            var energyRecovery = heroManager.getEnergyIncrease(bbm, minuteRest.toLong(), oldHouse)
            energyRecovery = heroManager.addEnergyKeepStage(bbm, energyRecovery)
            energiesRecovery[bbm.heroId] = energyRecovery
        }
        //tien hanh chuyen nha`.
        //danh sách bomber đang trong house > max slot nhà mới thì chọn ra
        val maxSlotNewHouse = newHouse.capacity
        if (lstBbm.size > maxSlotNewHouse) {
            val bbmMoveNewHouse = getListMoveNewHouse(lstBbm, maxSlotNewHouse)
            val bbmGoSleep = getListGoSleep(lstBbm, bbmMoveNewHouse)
            //chuyển trang thái cho danh sách bomber sleep
            for (bomberman in bbmGoSleep) {
                bomberman.stage = GameConstants.BOMBER_STAGE.SLEEP
            }
        }

        //update db thoi
        gameDataAccess.updateUserHouseStage(_dataType, _userId, arr)
        gameDataAccess.updateBombermanStage(_userId, lstBbm, energiesRecovery)
        return lstBbm
    }

    private fun getListMoveNewHouse(origin: List<Hero>, total: Int): List<Hero> {
        val result = mutableListOf<Hero>()
        //sort by tổng thuộc tính sau đó là năng lượng
        val sortedHeroes = origin.sortedWith(
            Comparator.comparing(Hero::totalProperties, Collections.reverseOrder()).thenComparing(Hero::energy)
        )
        for (i in 0 until total) {
            result.add(i, sortedHeroes[i])
        }
        return result
    }

    private fun getListGoSleep(lstBbm: List<Hero>, bbmMoveNewHouse: List<Hero>): List<Hero> {
        val result: MutableList<Hero> = ArrayList()
        for (bbmOgirin in lstBbm) {
            var isNewhouse = false
            for (bbm in bbmMoveNewHouse) {
                if (bbmOgirin.heroId == bbm.heroId) {
                    isNewhouse = true
                    break
                }
            }
            if (!isNewhouse) {
                result.add(bbmOgirin)
            }
        }
        return result
    }

    override fun buyHouseServer(rarity: Int): House {
        val housePrice = treasureHuntDataManager.getPriceHouse(_dataType)[rarity]
        // trừ reward
        val changeReason = if (_dataType == EnumConstants.DataType.TON) 
            ChangeRewardReason.BUY_HOUSE_TON 
        else 
            ChangeRewardReason.BUY_HOUSE_SOL
        
        gameDataAccess.subUserBlockReward(
            _userId, _dataType,
            EnumConstants.BLOCK_REWARD_TYPE.BCOIN_DEPOSITED,
            housePrice, changeReason
        )

        return createHouse(rarity, housePrice)
    }

    override fun buyHouseServerWithTokenNetwork(tokenNetwork: EnumConstants.DataType, rarity: Int): House {
        // Only check time restrictions for TON and SOL, RON and BAS are always allowed
        if (!tokenNetwork.isEthereumAirdropUser()) {
            if (Instant.now()
                    .toEpochMilli() > treasureHuntDataManager.getTimeDisableBuyWithTokenNetwork(tokenNetwork)
            ) {
                throw CustomException("Time buy with ${tokenNetwork.name} is ended")
            }
        }

        val housePrice = treasureHuntDataManager.getPriceHouseWithTokenNetwork(tokenNetwork)[rarity]
        
        val rewardType = tokenNetwork.convertToDepositType()
            
        val changeReason = when (tokenNetwork) {
            EnumConstants.DataType.TON -> ChangeRewardReason.BUY_HOUSE_TON
            EnumConstants.DataType.SOL -> ChangeRewardReason.BUY_HOUSE_SOL
            EnumConstants.DataType.RON -> ChangeRewardReason.BUY_HOUSE_RON
            EnumConstants.DataType.BAS -> ChangeRewardReason.BUY_HOUSE_BAS
            EnumConstants.DataType.VIC -> ChangeRewardReason.BUY_HOUSE_VIC
            else -> throw CustomException("Unsupported token network: ${tokenNetwork.name}")
        }
            
        gameDataAccess.subUserBlockReward(
            _userId, _dataType,
            rewardType,
            housePrice, changeReason
        )

        return createHouse(rarity, housePrice)
    }
    
    /**
     * Helper function to create a new house after payment has been processed
     */
    private fun createHouse(rarity: Int, housePrice: Float): House {
        // lấy house id từ database
        val houseId = gameDataAccess.getNextIdHouse()
        val houseStat = treasureHuntDataManager.getHouseStat(_dataType)[rarity]
        val houseDetail =
            HouseDetails(HouseDetails.genHouseDetail(houseId, 0, rarity, houseStat.recovery, houseStat.capacity))
        val house = House(houseDetail, false, 0L)

        // Thêm house trong database và server
        gameDataAccess.insertNewHouse(_dataType, _userId, house)
        thModeDataAccess.logUserAirdropBuyActivity(
            _userId,
            houseId.toString(),
            housePrice,
            "Buy House",
            _dataType
        )

        addHouse(house)
        return house
    }

    override fun reactiveHouseOldSeason(houseId: Int) {
        val house = gameDataAccess.getHouseOldSeason(_userId, houseId, _dataType)
        if (house == null) {
            throw CustomException("Wrong House Id")
        }
        house.isActive = false

        val housePrice: Float = ceil(treasureHuntDataManager.getPriceHouse(_dataType)[house.rarity] / 2)
        gameDataAccess.subUserBlockReward(
            _userId, _dataType,
            EnumConstants.BLOCK_REWARD_TYPE.BCOIN_DEPOSITED,
            housePrice, ChangeRewardReason.REACTIVE_HOUSE_OLD_SEASON
        )
        gameDataAccess.reactiveHouseOldSeason(_userId, houseId, _dataType)
        addHouse(house)
    }

    override fun rentHouse(houseId: Int, numDay: Int): Long {
        val house = getItems()[houseId] ?: throw Exception("You don't have this house")
        val housePackage: HouseRentPackage =
            _packageHouseRent.firstOrNull { it.rarity == house.rarity && it.numDays == numDay} ?: throw Exception("Package isn't exist")

        val endTime =
            LocalDateTime.now(ZoneOffset.UTC).plusDays(housePackage.numDays.toLong()).toInstant(ZoneOffset.UTC)

        val rewardType = if (_dataType == EnumConstants.DataType.RON)
            EnumConstants.BLOCK_REWARD_TYPE.RON_DEPOSITED
        else if (_dataType == EnumConstants.DataType.BAS)
            EnumConstants.BLOCK_REWARD_TYPE.BAS_DEPOSITED
        else if (_dataType == EnumConstants.DataType.VIC)
            EnumConstants.BLOCK_REWARD_TYPE.VIC_DEPOSITED
        else
            EnumConstants.BLOCK_REWARD_TYPE.BCOIN_DEPOSITED
        gameDataAccess.subUserBlockReward(
            _userId, _dataType,
            rewardType,
            housePackage.price, ChangeRewardReason.RENT_HOUSE
        )
        gameDataAccess.rentHouse(_userId, houseId, _dataType, endTime)
        house.endTimeRent = endTime.toEpochMilli()
        return house.endTimeRent
    }

    override fun heroRestInHouse(heroId: Int): House? {
        val house = findHouseRest() ?: return null
        if (activeHouse != null && house.houseId == activeHouse!!.houseId){
            return house
        }
        _heroHouseRent[heroId] = house.houseId
        gameDataAccess.heroRestHouseRent(heroId, house.houseId)
        return house
    }

    override fun heroGoWorkFromHouseRent(heroId: Int): House? {
        val houseId = _heroHouseRent[heroId] ?: return null
        _heroHouseRent.remove(heroId)
        gameDataAccess.heroGoWorkFromHouseRent(heroId, houseId)
        return getItems()[houseId]
    }

    override fun getHouseHeroRest(hero: Hero): House? {
        if (hero.stage != GameConstants.BOMBER_STAGE.HOUSE) {
            throw Exception("Hero is not in house")
        }
        if (_heroHouseRent.containsKey(hero.heroId)) {
            return getItems()[_heroHouseRent[hero.heroId]]
        }
        return activeHouse
    }

    override fun getHeroInHouse(): ISFSArray {
        if (activeHouse == null) {
            return SFSArray()
        }
        // đưa về map với key là house, value là list hero
        val heroInHouseActive =
            heroManager.housingHeroes.filter { !_heroHouseRent.keys.contains(it.heroId) }.map { it.heroId }
        val heroInHouse = _heroHouseRent.entries.groupBy({ it.value }, { it.key }).toMutableMap()
        heroInHouse[activeHouse!!.houseId] = heroInHouseActive
        return heroInHouse.toSFSArray { entry ->
            SFSObject().apply {
                putInt("house_id", entry.key)
                putIntArray("list_heroes", entry.value)
            }
        }
    }

    private fun findHouseRest(): House? {
        val houseCanRest = mutableListOf<House>()
        if (activeHouse != null) {
            val heroInHouseActive =
                heroManager.housingHeroes.filter { !_heroHouseRent.keys.contains(it.heroId) }
            if (heroInHouseActive.size < activeHouse!!.capacity) {
                houseCanRest.add(activeHouse!!)
            }
        }
        val listHouseRent =
            getItems().values.filter { (it.endTimeRent != 0L && it.endTimeRent > System.currentTimeMillis()) }
        houseCanRest.addAll(listHouseRent.filter { houseRent -> _heroHouseRent.values.count { it == houseRent.houseId } < houseRent.capacity })

        if (houseCanRest.isEmpty()) {
            return null
        }
        return houseCanRest.maxBy { it.rarity }
    }
}