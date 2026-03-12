package com.senspark.game.manager.hero

import com.senspark.game.api.BlockchainHeroResponse
import com.senspark.game.data.model.ServerHeroDetails
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.House
import com.senspark.game.data.model.nft.IHeroDetails
import com.senspark.game.declare.EnumConstants.*
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject

interface IUserHeroFiManager {
    val housingHeroes: List<Hero>
    val activeHeroes: List<Hero>
    val activeHeroCount: Int
    val trialHeroes: List<Hero>
    val fiHeroes: List<Hero>
    val traditionalHeroes: List<Hero>
    val tonHeroes: List<Hero>
    val solHeroes: List<Hero>
    val ronHeroes: List<Hero>
    val basHeroes: List<Hero>
    val vicHeroes: List<Hero>
    fun sendServerHeroToClient(dataType: DataType): ISFSObject
    fun getBombermans(): Map<Int, Hero>
    fun toArray(): List<Hero>
    fun hasBomberman(id: Int): Boolean
    fun getHero(id: Int, heroType: HeroType): Hero?
    fun getHero(id: Int, dataType: DataType): Hero?
    fun getAllHeroTon(): List<Hero>
    fun getAllHeroSol(): List<Hero>
    fun getAllHeroFi(): List<Hero>
    fun getAllHeroRon(): List<Hero>
    fun getAllHeroBas(): List<Hero>
    fun getAllHeroVic(): List<Hero>
    fun addBomberman(hero: Hero)
    fun removeBomberman(id: Int)
    fun removeTrialBomberman()

    /**
     * set hero go sleep và tính lại năng lượng
     *
     * @param bbm            bbm
     * @param userController userController
     * @return energyRecovery
     */
    fun setSleep(bbm: Hero): Int
    fun setGoHouse(bbm: Hero): Int?
    fun setWork(bbm: Hero): Int
    fun getMinuteRest(bbm: Hero): Int
    fun addEnergy(bbm: Hero, energyIncrease: Int): Int
    fun addEnergyKeepStage(bbm: Hero, energyIncrease: Int): Int
    fun addEnergyAndSetWorking(bbm: Hero, energyIncrease: Int): Int
    fun getEnergyIncrease(bbm: Hero, minutes: Long, uHouse: House?): Int
    fun syncBomberMan(): ISFSObject
    fun syncBomberManV3(): ISFSObject
    fun syncHeroAndGetResponse(dataSync: List<BlockchainHeroResponse>): ISFSObject
    fun repairShield(rewardType: BLOCK_REWARD_TYPE, heroId: Int): ISFSObject
    fun addHeroesServer(detailList: List<ServerHeroDetails>): List<Pair<Int, HeroType>>
    fun isBuyHeroesTrial(): Boolean
    fun updateStakeAmountHeroes(hero: Hero, stakeBcoin: Double, stakeSen: Double)
    fun buyHeroServer(quantity: Int, rewardType: BLOCK_REWARD_TYPE): ISFSArray
    fun claimHeroServer(quantity: Int): ISFSArray
    fun fusionHeroServer(
        targetRarity: Int,
        heroList: List<Int>,
        percent: Int,
        priceFusion: Double,
        dataType: DataType
    ): ISFSObject
    fun multiFusionHeroServer(targetRarity: Int, heroList: List<Int>, rarity: Int): ISFSObject
}