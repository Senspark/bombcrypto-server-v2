package com.senspark.game.manager.hero

import com.senspark.common.utils.toSFSArray
import com.senspark.game.constant.ItemStatus
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.manager.grindHero.IGrindHeroManager
import com.senspark.game.data.manager.hero.IConfigHeroTraditionalManager
import com.senspark.game.data.manager.hero.IHeroBuilder
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.manager.upgradeHero.IUpgradeHeroTrManager
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.user.AddUserItemWrapper
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IRewardDataAccess
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.db.helper.QueryHelper
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.HeroTRType
import com.senspark.game.declare.customEnum.MissionAction
import com.senspark.game.declare.customEnum.UpgradeHeroType
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.blockReward.IUserBlockRewardManager
import com.senspark.game.manager.dailyMission.IUserMissionManager
import com.senspark.game.manager.material.IUserMaterialManager
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.time.Instant
import java.time.temporal.ChronoUnit


const val COOL_DOWN_RELOAD_HERO = 3600000L //1h


data class ItemHero(
    val itemId: Int,
    val heroes: MutableMap<ItemStatus, MutableList<Hero>> = mutableMapOf()
) {
    fun addHero(hero: Hero): ItemHero {
        val status = ItemStatus.fromValue(hero.status)
        heroes[status] = (heroes[status] ?: mutableListOf()).apply {
            add(hero)
        }
        return this
    }

    fun toSfsObject(): ISFSObject {
        return SFSObject().apply {
            putInt("item_id", itemId)
            putSFSArray("heroes", heroes.toSFSArray {
                SFSObject().apply {
                    putInt("status", it.key.value)
                    putInt("quantity", it.value.size)
                    if (it.value.isNotEmpty()) {
                        putInt("hero_id", it.value[0].heroId)
                        putBool("is_active", it.value[0].isActive)
                        putInt("upgraded_speed", it.value[0].upgradedSpeed)
                        putInt("upgraded_bomb", it.value[0].upgradedBomb)
                        putInt("upgraded_range", it.value[0].upgradedRange)
                        putInt("upgraded_hp", it.value[0].upgradedHp)
                        putInt("upgraded_dmg", it.value[0].upgradedDmg)
                        putInt("max_upgrade_speed", it.value[0].maxUpgradeSpeed)
                        putInt("max_upgrade_bomb", it.value[0].maxUpgradeBomb)
                        putInt("max_upgrade_range", it.value[0].maxUpgradeRange)
                        putInt("max_upgrade_dmg", it.value[0].maxUpgradeDmg)
                        putInt("max_upgrade_hp", it.value[0].maxUpgradeHp)
                    }
                }
            })
        }
    }
}

class UserHeroTRManager(
    private val _mediator: UserControllerMediator,
    private val userBlockRewardManager: IUserBlockRewardManager,
    private val userMaterialManager: IUserMaterialManager,
    private val userMissionManager: IUserMissionManager,
) : IUserHeroTRManager {

    private val configHeroTraditionalManager = _mediator.svServices.get<IConfigHeroTraditionalManager>()
    private val upgradeHeroTrManager = _mediator.svServices.get<IUpgradeHeroTrManager>()
    private val grindHeroManager = _mediator.svServices.get<IGrindHeroManager>()
    private val configItemManager = _mediator.svServices.get<IConfigItemManager>()
    private val heroBuilder = _mediator.svServices.get<IHeroBuilder>()

    private val dataAccessManager = _mediator.services.get<IDataAccessManager>()
    private val rewardDataAccess: IRewardDataAccess = dataAccessManager.rewardDataAccess
    private val userDataAccess: IUserDataAccess = dataAccessManager.userDataAccess
    
    private val locker = Any()
    private var _heroes: Map<Int, Hero> = emptyMap()
    private var _heroesMapByType: MutableMap<HeroTRType, MutableMap<Int, ItemHero>> = mutableMapOf()
    private var lastTimeLoadHero: Instant? = null

    init {
        loadHero()
    }

    override fun loadHero(loadImmediately: Boolean) {
        synchronized(locker) {
            lastTimeLoadHero.run {
                if (loadImmediately
                    || this == null
                    || Instant.now().isAfter(this.plus(COOL_DOWN_RELOAD_HERO, ChronoUnit.MILLIS))
                ) {
                    _heroes = heroBuilder.getHeroTraditional(_mediator.userId, configHeroTraditionalManager)
                    _heroesMapByType.clear()
                    _heroes.values.forEach {
                        val itemId = it.heroConfig?.itemId ?: throw CustomException("Cannot find heroConfig")
                        val heroTRType = it.heroTrType
                        val heroByType = _heroesMapByType[heroTRType] ?: mutableMapOf()
                        heroByType[itemId] = (heroByType[itemId] ?: ItemHero(itemId)).addHero(it)
                        _heroesMapByType[heroTRType] = heroByType
                    }
                    lastTimeLoadHero = Instant.now()
                }
            }
        }
    }

    override val heroes: Map<Int, Hero>
        get() {
            loadHero()
            return _heroes
        }

    override val heroesMapByType: MutableMap<HeroTRType, MutableMap<Int, ItemHero>>
        get() {
            loadHero()
            return _heroesMapByType
        }

    override val heroesMapByItemId: Map<Int, ItemHero>
        get() = heroesMapByType[HeroTRType.HERO] ?: emptyMap()

    override val heroesSoulMapByItemId: Map<Int, ItemHero>
        get() = heroesMapByType[HeroTRType.SOUL] ?: emptyMap()

    override fun getHero(id: Int): Hero {
        return _heroes[id] ?: throw CustomException("Hero tr $id not exists")
    }

    override fun active(id: Int) {
        val hero = getHero(id)
        require(hero.heroTrType == HeroTRType.HERO) {
            "Only one type HERO can active"
        }
        if (!hero.isActive) {
            userDataAccess.activeHeroTr(_mediator.userId, hero.heroId)
            loadHero(true)
        }
    }

    override fun canGetFreeHeroTR(): Boolean {
        synchronized(locker) {
            // Cần load lại data từ database để lấy đc data mới nhất vì có nhiều network có thể thay đổi data này
            loadHero(true)
            return _heroes.isEmpty()
        }
    }

    override fun grindHero(itemId: Int, quantity: Int, itemStatus: ItemStatus): ISFSArray {
        // Do có nhiều client nên trước khi grind hero phải load lại từ db
        loadHero(true)
        val item = configItemManager.getItem(itemId)
        val grindReward = grindHeroManager.grind(item.kind, quantity)
        val goldFee = grindReward.first.price.times(quantity)
        if (goldFee > userBlockRewardManager.getTotalGoldHaving()) {
            throw CustomException("Not enough $goldFee ${EnumConstants.BLOCK_REWARD_TYPE.GOLD.name}")
        }
        val heroes = heroesSoulMapByItemId[itemId]?.heroes?.get(itemStatus) ?: emptyList()
        if (heroes.size < quantity) {
            throw CustomException("Not enough $quantity hero souls")
        }
        val dropHeroIds = heroes.map { it.heroId }.take(quantity)
        val rewardAdd = grindReward.second.groupBy { it.item }.map {
            AddUserItemWrapper(
                it.key,
                it.value.sumOf { it2 -> it2.quantity },
            )
        }
        val rewardSpend = mapOf(EnumConstants.BLOCK_REWARD_TYPE.GOLD to goldFee.toFloat())
        //save 
        rewardDataAccess.addTRRewardForUser(
            _mediator.userId, _mediator.dataType, rewardAdd, {}, "Grind_hero", rewardSpend,
            additionUpdateQueries = listOf(QueryHelper.queryDeleteHeroTRAndLogGrind(_mediator.userId, itemId, dropHeroIds))
        )
        // mark complete mission
        userMissionManager.completeMission(listOf(Pair(MissionAction.GRIND_HERO, quantity)))
        //reload reward, hero, material
        userBlockRewardManager.loadUserBlockReward()
        loadHero(true)
        userMaterialManager.loadMaterials()
        return rewardAdd.sortedBy { it.item.id }.toSFSArray {
            SFSObject().apply {
                putInt("item_id", it.item.id)
                putInt("quantity", it.quantity)
            }
        }
    }

    override fun upgradeHero(heroId: Int, upgradeType: UpgradeHeroType): ISFSObject {
        val hero = heroes[heroId] ?: throw CustomException("Hero not exists")
        if (hero.heroTrType != HeroTRType.HERO) {
            throw CustomException("Only can upgrade type hero")
        }
        val config = upgradeHeroTrManager.get(upgradeType, hero)
        userBlockRewardManager.checkEnoughReward(config.gemFee.toFloat(), EnumConstants.BLOCK_REWARD_TYPE.GEM)
        userBlockRewardManager.checkEnoughReward(config.goldFee.toFloat(), EnumConstants.BLOCK_REWARD_TYPE.GOLD)
        config.items.forEach { userMaterialManager.checkEnoughCrystal(it.itemId, it.quantity) }
        this.checkMaxUpgrade(upgradeType, hero)
        //save
        userDataAccess.upgradeHeroTr(
            _mediator.userId,
            hero.heroId,
            hp = if (upgradeType == UpgradeHeroType.HP) 1 else 0,
            dmg = if (upgradeType == UpgradeHeroType.DMG) 1 else 0,
            speed = if (upgradeType == UpgradeHeroType.SPEED) 1 else 0,
            range = if (upgradeType == UpgradeHeroType.RANGE) 1 else 0,
            bomb = if (upgradeType == UpgradeHeroType.BOMB) 1 else 0,
            config
        )
        //save complete mission
        userMissionManager.completeMission(listOf(Pair(MissionAction.UPGRADE_HERO, 1)))

        userBlockRewardManager.loadUserBlockReward()
        userMaterialManager.loadMaterials()
        loadHero(true)
        return heroes[heroId]?.toSFSObject() ?: throw CustomException("Hero not exists")

    }

    private fun checkMaxUpgrade(upgradeType: UpgradeHeroType, hero: Hero) {
        val currentValue: Int
        val maxValue: Int
        when (upgradeType) {
            UpgradeHeroType.SPEED -> {
                currentValue = hero.maxSpeed
                maxValue = hero.maxUpgradeSpeed
            }

            UpgradeHeroType.RANGE -> {
                currentValue = hero.maxRange
                maxValue = hero.maxUpgradeRange
            }

            UpgradeHeroType.BOMB -> {
                currentValue = hero.maxBomb
                maxValue = hero.maxUpgradeBomb
            }

            UpgradeHeroType.DMG -> {
                currentValue = hero.dmg
                maxValue = hero.maxUpgradeDmg
            }

            UpgradeHeroType.HP -> {
                currentValue = hero.hp
                maxValue = hero.maxUpgradeHp
            }
        }
        if (currentValue >= maxValue) {
            throw CustomException("This stat has reached its limit")
        }
    }

    override fun isHavingHero(id: Int): Boolean {
        return _heroes.containsKey(id)
    }
}