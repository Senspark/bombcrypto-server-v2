package com.senspark.game.data.manager.hero

import com.senspark.game.data.model.ServerHeroDetails
import com.senspark.game.data.model.nft.*
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.declare.EnumConstants
import com.senspark.game.manager.stake.IHeroStakeManager
import com.senspark.game.service.IHeroUpgradeShieldManager
import com.senspark.game.utils.Extractor
import com.smartfoxserver.v2.entities.data.ISFSObject
import java.time.Instant
import java.util.ArrayList
import java.util.HashMap

class HeroBuilder(
    private val _gameDataAccess: IGameDataAccess,
    heroStakeManager: IHeroStakeManager,
    heroAbilityConfigManager: IHeroAbilityConfigManager,
    heroUpgradePowerManager: IHeroUpgradePowerManager,
    heroUpgradeShieldManager: IHeroUpgradeShieldManager
) : IHeroBuilder {

    private val _heroShieldBuilder: HeroShieldBuilder = HeroShieldBuilder(
        heroUpgradeShieldManager
    )

    private val _helper: HeroHelper = HeroHelper(
        heroStakeManager,
        heroAbilityConfigManager,
        heroUpgradePowerManager,
        _heroShieldBuilder
    )

    override fun initialize() {
    }

    override fun getFiHeroes(uid: Int, dataType: EnumConstants.DataType, limit: Int, offset: Int): Map<Int, Hero> {
        val sfsArray = _gameDataAccess.getFiHeroes(uid, dataType, limit, offset)
        val size = sfsArray.size()
        val result: MutableMap<Int, Hero> = HashMap()

        for (i in 0 until size) {
            val rs = sfsArray.getSFSObject(i)
            val hero = fromSFSObject(rs)
            result[Extractor.parseHeroId(hero.heroId, hero.type)] = hero
        }
        return result.toMap()
    }

    override fun getFiHeroes(
        uid: Int,
        dataTypes: List<EnumConstants.DataType>,
        lstBbmId: List<Int>,
        type: EnumConstants.HeroType,
        listItemIds: List<Int>,
        limit: Int,
        offset: Int
    ): List<Hero> {
        val result: MutableList<Hero> = ArrayList()
        val sfsArray = _gameDataAccess.getFiHeroes(uid, dataTypes, lstBbmId, type, listItemIds, limit, offset)
        val size = sfsArray.size()
        for (i in 0 until size) {
            val rs = sfsArray.getSFSObject(i)
            result.add(fromSFSObject(rs))
        }
        return result
    }

    override fun getTonHeroes(uid: Int, limit: Int): Map<Int, Hero> {
        val sfsArray = _gameDataAccess.getTonHeroes(uid, limit)
        val size = sfsArray.size()
        val result: MutableMap<Int, Hero> = HashMap()

        for (i in 0 until size) {
            val rs = sfsArray.getSFSObject(i)
            val hero = fromSFSObject(rs)
            result[Extractor.parseHeroId(hero.heroId, hero.type)] = hero
        }
        return result.toMap()
    }

    override fun getHeroesOldSeason(uid: Int, type: EnumConstants.HeroType): List<Hero> {
        val sfsArray = _gameDataAccess.getHeroesOldSeason(uid, type)
        val size = sfsArray.size()
        val result = mutableListOf<Hero>()

        for (i in 0 until size) {
            val rs = sfsArray.getSFSObject(i)
            val hero = fromSFSObject(rs)
            result.add(hero)
        }
        return result
    }

    override fun getHeroTraditional(
        uid: Int,
        configHeroTraditionalManager: IConfigHeroTraditionalManager
    ): MutableMap<Int, Hero> {
        val result = mutableMapOf<Int, Hero>()
        val sfsArray = _gameDataAccess.getHeroTraditional(uid, configHeroTraditionalManager)
        val size = sfsArray.size()
        for (i in 0 until size) {
            val rs = sfsArray.getSFSObject(i)
            val hero = fromSFSObject(rs, configHeroTraditionalManager)
            result[hero.heroId] = hero
        }
        return result
    }

    override fun getHeroFiFromDatabase(dataType: EnumConstants.DataType, bomberId: List<Int>, type: Int): List<Hero> {
        val sfsArray = _gameDataAccess.getHeroFiFromDatabase(dataType, bomberId, type)
        val result = mutableListOf<Hero>()
        for (i in 0 until sfsArray.size()) {
            val sfsObject = sfsArray.getSFSObject(i)
            result.add(fromSFSObject(sfsObject))
        }
        return result
    }

    private fun fromSFSObject(obj: ISFSObject): Hero {
        val type = obj.getInt("type")
        val validHeroTypes =
            listOf(EnumConstants.HeroType.FI.value, 
                EnumConstants.HeroType.TON.value, 
                EnumConstants.HeroType.SOL.value, 
                EnumConstants.HeroType.RON.value, 
                EnumConstants.HeroType.VIC.value, 
                EnumConstants.HeroType.BAS.value)
        require(type in validHeroTypes) {
            "Hero type must be FI or TON or SOL or RON or VIC or BAS"
        }
        val dataType = EnumConstants.DataType.valueOf(obj.getUtfString("data_type"))
        val details: IHeroDetails =
            if (dataType.isAirdropUser()) {
                ServerHeroDetails(obj)
            } else {
                BlockchainHeroDetails(obj.getUtfString("gen_id"), dataType)
            }
        return createHero(obj, details)
    }

    private fun fromSFSObject(obj: ISFSObject, traditionalManager: IConfigHeroTraditionalManager): Hero {
        require(obj.getInt("type") == EnumConstants.HeroType.TR.value) {
            "Hero type must be TR"
        }
        val dataType = EnumConstants.DataType.valueOf(obj.getUtfString("data_type"))
        val details = NonFiHeroDetails(obj, dataType, traditionalManager)
        return createHero(obj, details)
    }

    override fun newInstance(userId: Int, details: IHeroDetails): Hero {
        return Hero(
            userId,
            details,
            false,
            0,
            details.stamina * 50,
            Instant.now().toEpochMilli(),
            _heroShieldBuilder.create(details),
            0.0,
            0.0,
            Instant.EPOCH,
            0,
            _helper
        )
    }

    override fun createHero(obj: ISFSObject, details: IHeroDetails): Hero {
        val shield = _heroShieldBuilder.fromString(
            details.rarity,
            obj.getUtfString("shield") ?: "[]",
            if (obj.containsKey("shield_level")) obj.getInt("shield_level") else 0
        )
        val lockSince = Extractor.tryGet<Instant>(obj, "lock_since", Instant.EPOCH)
        val lockSeconds = Extractor.tryGet<Int>(obj, "lock_seconds", 0)

        return Hero(
            obj.getInt("uid"),
            details,
            obj.getInt("active") == 1,
            obj.getInt("stage"),
            obj.getInt("energy"),
            obj.getLong("time_rest") ?: 0,
            shield,
            obj.getDouble("stake_amount") ?: 0.0,
            obj.getDouble("stake_sen") ?: 0.0,
            lockSince,
            lockSeconds,
            _helper
        )
    }
}