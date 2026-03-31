package com.senspark.game.manager.resourceSync.hero

import com.senspark.game.api.BlockchainHeroResponse
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.manager.hero.IConfigHeroTraditionalManager
import com.senspark.game.data.manager.hero.IHeroBuilder
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.IHeroDetails
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.EnumConstants.HeroType
import com.senspark.game.declare.GameConstants
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.blockChain.BlockchainResponseManager.NewOrUpdatedHero
import com.senspark.game.manager.stake.IHeroStakeManager
import com.senspark.game.service.IAllHeroesFiManager
import com.senspark.game.utils.Extractor
import com.senspark.lib.data.manager.IGameConfigManager
import java.util.concurrent.ConcurrentHashMap

data class HeroSyncData(
    val newDetail: MutableList<IHeroDetails>,
    val changedHero: List<IHeroDetails>,
)

class HeroSyncService(
    private val _gameDataAccess: IGameDataAccess,
    private val _userDataAccess: IUserDataAccess,
    private val _gameConfigManager: IGameConfigManager,

    private val _heroStakeManager: IHeroStakeManager,
    private val _heroBuilder: IHeroBuilder,
    private val _allHeroesFiManager: IAllHeroesFiManager,
    private val _traditionalManager: IConfigHeroTraditionalManager,

    ) : IHeroSyncService {

    override fun syncHero(
        mediator: UserControllerMediator,
        currentHero: ConcurrentHashMap<Int, Hero>,
        dataSync: List<BlockchainHeroResponse>
    ): HeroSyncData {
        if (mediator.dataType.isAirdropUser()) {
            throw CustomException("Not support sync hero for airdrop user")
        }

        val detailsSynced = dataSync.map { it.details }
        val heroesAmountAfter = detailsSynced.size
        val heroesAmountBefore = currentHero.size

        // Deleted heroes.
        val deleted = deleteHeroNotExitAndUpdateServer(mediator, currentHero, detailsSynced.toList())

        // New / changed heroes.
        val newDetails: MutableList<IHeroDetails> = ArrayList()
        val changedHeroes: MutableList<Hero> = ArrayList()
        val repairedHeroes: MutableMap<Int, Int> = HashMap()
        for (detail in detailsSynced) {
            val hero = currentHero[Extractor.parseHeroId(detail.heroId, detail.type)]
            if (hero == null) {
                newDetails.add(detail)
            } else {
                if (hero.details.isEqualTo(detail)) {
                    // No change.
                } else {
                    changedHeroes.add(hero)
                    if (hero.shield.level != detail.shieldLevel) {
                        hero.updateShieldLevel(detail.shieldLevel)
                    }
                    checkAndRepairShield(repairedHeroes, hero, detail)
                }
                hero.updateDetails(detail)
            }
        }
        updateHeroes(mediator, changedHeroes)
        addHeroesBlockchain(mediator, newDetails, currentHero)
        for ((detail, stakeAmount, stakeSen) in dataSync) {
            val hero = currentHero[Extractor.parseHeroId(detail.heroId, detail.type)] ?: break
            updateStakeAmountHeroes(hero, stakeAmount, stakeSen)
        }
        logRepairShield(mediator, repairedHeroes)

        mediator.logger.log2("${mediator.userName} SYNC_BOMBERMAN_V2", {
            val allSyncedHeroes = detailsSynced.map { it.heroId }.joinToString(", ")
            val newIds = if (newDetails.isNotEmpty())
                newDetails.map { it.heroId }.joinToString(", ") else ""
            val deletedIds = if (deleted.isNotEmpty())
                deleted.joinToString(", ") else ""
            "Before: $heroesAmountBefore, After: $heroesAmountAfter, Removed: ${deleted.size}, New: ${newDetails.size}," +
                    " Changed: ${changedHeroes.size}, Repaired: ${repairedHeroes.size}" +
                    "\nTotal ids: $allSyncedHeroes\nNew ids: $newIds\nDeleted ids: $deletedIds"
        })

        return HeroSyncData(newDetails, changedHeroes.map { it.details })
    }

    override fun syncHeroOffline(uid: Int, dataType: DataType, dataSync: List<BlockchainHeroResponse>) {
        // B1: Load toàn bộ hero của user này trong db lên để sử dụng
        val allHero = _heroBuilder.getFiHeroes(uid, dataType, 1000000, 0)

        // B2: Kiểm tra và xoá các hero ko còn tồn tại trong dữ liệu từ blockchain
        val listHeroNeedDelete = deleteHeroNotExit(allHero, dataSync)
        _gameDataAccess.updateBombermanNotExist(uid, dataType, listHeroNeedDelete)

        // B3: Cập nhật các hero mới hoặc đã thay đổi
        val (newHeroes, updateHero) = getNewAndUpdatedHeroes(uid, allHero, dataSync)
        // Thêm hero mới vào database
        if (newHeroes.isNotEmpty()) {
            _gameDataAccess.insertNewBomberman(uid, dataType, newHeroes)

        }
        // Update hero có database thay đổi
        if (updateHero.isNotEmpty()) {
            _gameDataAccess.updateHeroDetails(uid, dataType, updateHero)
        }
    }

    override fun updateStakeAmountHeroes(hero: Hero, stakeBcoin: Double, stakeSen: Double) {
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

    /**
     * Thêm heroes vào database (bad optimization)
     */
    private fun addHeroesBlockchain(
        mediator: UserControllerMediator,
        detailList: List<IHeroDetails>,
        currentHero: ConcurrentHashMap<Int, Hero>
    ): List<Pair<Int, HeroType>> {
        if (mediator.dataType.isAirdropUser()) {
            throw CustomException("Not support sync hero for airdrop user")
        }

        if (detailList.isEmpty()) {
            return emptyList()
        }
        val activeHereCount = currentHero.values.filter { it.isActive && it.type != HeroType.TR }.size

        val allHeroType = detailList.first().type
        val curBomberActive = activeHereCount
        var bomberActiveLeft = _gameConfigManager.maxBomberActive - curBomberActive
        val newHeroIds = mutableListOf<Pair<Int, HeroType>>()
        for (details in detailList) {
            val hero = _heroBuilder.newInstance(mediator.userId, details)
            val type = hero.type
            if (type != HeroType.FI) hero.stage = GameConstants.BOMBER_STAGE.SLEEP
            if (bomberActiveLeft > 0) {
                hero.isActive = true
            }
            val resultValue = _gameDataAccess.insertNewBomberman(
                mediator.userName,
                hero,
                mediator.dataType,
                0
            )
            if (resultValue != null) {
                if (resultValue.isLocked) {
                    hero.isActive = false
                    hero.updateLockUntil(resultValue.lockUtil)
                    _allHeroesFiManager.getSubManager(resultValue.oldOwnerUid, mediator.dataType)
                        ?.removeBomberman(Extractor.parseHeroId(hero.heroId, hero.type))
                }
                resultValue.let { newHeroIds.add(Pair(it.bombermanId, type)) }
            }
            if (hero.isActive) {
                --bomberActiveLeft
            }
        }

        // Reload data from database.
        val heroIds: MutableList<Int> = ArrayList()
        for (details in detailList) {
            heroIds.add(details.heroId)
        }
        val heroes: List<Hero> = _heroBuilder.getFiHeroes(
            mediator.userId,
            listOf(mediator.dataType, DataType.TR),
            heroIds,
            allHeroType,
            _traditionalManager.itemIds,
            1000000,
            0
        )
        for (hero in heroes) {
            currentHero[Extractor.parseHeroId(hero.heroId, hero.type)] = hero
        }
        return newHeroIds
    }

    private fun deleteHeroNotExitAndUpdateServer(
        mediator: UserControllerMediator,
        currentHero: ConcurrentHashMap<Int, Hero>,
        detailList: List<IHeroDetails>
    ): List<Int> {
        val heroIds: MutableList<Int> = ArrayList()
        val heroes = currentHero.values.toList()
        for (hero in heroes) {
            var has = false
            for (details in detailList) {
                //neu hero trial thi khong delete
                if (hero.heroId == details.heroId || hero.type != HeroType.FI) {
                    has = true
                    break
                }
            }
            if (!has) {
                heroIds.add(hero.heroId)
            }
        }
        if (heroIds.isNotEmpty()) {
            //remove tren server
            for (idHero in heroIds) {
                val id = Extractor.parseHeroId(idHero, HeroType.FI)
                if (currentHero.containsKey(id)) {
                    currentHero.remove(id)
                }
            }
        }

        //delete bbm tren DB
        _gameDataAccess.updateBombermanNotExist(mediator.userId, mediator.dataType, heroIds)
        return heroIds
    }

    private fun deleteHeroNotExit(allHero: Map<Int, Hero>, dataSync: List<BlockchainHeroResponse>): List<Int> {
        val heroIds: MutableList<Int> = ArrayList()
        for (hero in allHero) {
            var has = false
            for (sync in dataSync) {
                //neu hero trial thi khong delete
                if (hero.value.heroId == sync.details.heroId || hero.value.type != HeroType.FI) {
                    has = true
                    break
                }
            }
            if (!has) {
                heroIds.add(hero.value.heroId)
            }
        }
        return heroIds
    }

    private fun getNewAndUpdatedHeroes(
        uid: Int,
        allHero: Map<Int, Hero>,
        dataSync: List<BlockchainHeroResponse>
    ): NewOrUpdatedHero {
        val newHero = mutableListOf<Hero>()
        val updatedHero = mutableListOf<Hero>()

        for (sync in dataSync) {
            val detail = sync.details
            val heroId = detail.heroId
            val heroType = detail.type
            val id = Extractor.parseHeroId(heroId, heroType)
            if (allHero.containsKey(id)) {
                // Hero đã tồn tại, kiểm tra xem có cần cập nhật không
                val existingHero = allHero[id] ?: continue
                // Cập nhật hero nếu detail khác
                if (!existingHero.details.isEqualTo(detail)) {
                    if (existingHero.shield.level != detail.shieldLevel) {
                        existingHero.updateShieldLevel(detail.shieldLevel)
                    }
                    // Check và repair shield ở đây
                    if (existingHero.resetShieldCounter != detail.resetShieldCounter) {
                        existingHero.resetShieldToFull(GameConstants.BOMBER_ABILITY.AVOID_THUNDER)
                    }

                    // Update stake ở đây
                    existingHero.stakeBcoin = sync.stakeBcoin
                    existingHero.stakeSen = sync.stakeSen
                    val minStakeHeroConfig = _heroStakeManager.minStakeHeroConfig

                    //Update shield cho hero L
                    if (!existingHero.isHeroS) {
                        // không phải hero S thì kiểm tra để thêm shield
                        if (existingHero.stakeBcoin >= minStakeHeroConfig[existingHero.rarity]!!) {
                            val shieldInDatabase = _gameDataAccess.getShieldHeroFromDatabase(
                                detail.dataType, existingHero.heroId,
                                existingHero.type.value
                            )
                            if (shieldInDatabase == "[]") {
                                existingHero.addBasicShield()
                            }
                        }
                    }

                    existingHero.updateDetails(detail)
                    updatedHero.add(existingHero)
                }
            } else {
                // Hero mới, thêm vào danh sách mới
                val hero = _heroBuilder.newInstance(uid, detail)
                newHero.add(hero)
            }
        }

        return NewOrUpdatedHero(newHero, updatedHero)
    }

    private fun checkAndRepairShield(
        repairedHeroes: MutableMap<Int, Int>,
        hero: Hero,
        syncedDetail: IHeroDetails
    ) {
        if (hero.resetShieldCounter != syncedDetail.resetShieldCounter) {
            repairedHeroes[hero.heroId] =
                hero.shield.getCapacity(GameConstants.BOMBER_ABILITY.AVOID_THUNDER)
            hero.resetShieldToFull(GameConstants.BOMBER_ABILITY.AVOID_THUNDER)
        }
    }

    private fun updateHeroes(mediator: UserControllerMediator, listChangedHero: List<Hero>) {
        for (hero in listChangedHero) {
            _gameDataAccess.updateHeroDetails(mediator.userId, mediator.dataType, hero)
        }
    }

    private fun logRepairShield(mediator: UserControllerMediator, repairShield: Map<Int, Int>) {
        _userDataAccess.logRepairShield(mediator.userName, mediator.dataType, repairShield)
    }
}