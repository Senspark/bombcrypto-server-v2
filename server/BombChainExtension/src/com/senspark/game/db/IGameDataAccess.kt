package com.senspark.game.db

import com.senspark.common.service.IGlobalService
import com.senspark.game.controller.MapData
import com.senspark.game.data.PvPData
import com.senspark.game.data.PvPHeroEnergyData
import com.senspark.game.data.manager.hero.IConfigHeroTraditionalManager
import com.senspark.game.data.model.adventrue.UserAdventureMode
import com.senspark.game.data.model.config.OfflineRewardTHModeConfig
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.House
import com.senspark.game.data.model.user.UserBlockReward
import com.senspark.game.declare.EnumConstants.*
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSArray
import java.sql.SQLException
import java.time.Instant

data class InsertNewBombermanResult(
    val bombermanId: Int,
    val oldOwnerUid: Int,
    val isLocked: Boolean,
    val lockUtil: Instant,
)

interface IGameDataAccess : IGlobalService {
    fun getFiHeroes(uid: Int, dataType: DataType, limit: Int, offset: Int): ISFSArray
    fun getFiHeroes(
        uid: Int,
        dataTypes: List<DataType>,
        lstBbmId: List<Int>,
        type: HeroType,
        listItemIds: List<Int>,
        limit: Int,
        offset: Int
    ): ISFSArray

    fun getTonHeroes(uid: Int, limit: Int): ISFSArray
    fun getHeroesOldSeason(uid: Int, type: HeroType): ISFSArray
    fun insertNewBomberman(userName: String, hero: Hero, dataType: DataType, itemId: Int): InsertNewBombermanResult?
    fun insertNewServerHero(userName: String, hero: Hero, dataType: DataType): Int
    fun updateBombermanNotExist(uid: Int, dataType: DataType, heroIds: List<Int>)
    fun updateHeroDetails(uid: Int, dataType: DataType, hero: Hero): Boolean
    fun insertNewBomberman(uid: Int, dataType: DataType, heroes: List<Hero>): Boolean
    fun updateHeroDetails(uid: Int, dataType: DataType, heroes: List<Hero>): Boolean

    @Throws(CustomException::class)
    fun loadMapData(uid: Int, dataType: DataType): Map<MODE, MapData>

    fun loadSingleMapData(uid: Int, dataType: DataType, mode: MODE): MapData?

    @Throws(CustomException::class)
    fun insertMapData(
        uid: Int,
        data: String,
        dataType: DataType,
        dateCreate: Long,
        tileset: Int,
        mode: MODE
    ): Boolean

    fun updateMapData(uid: Int, mapData: MutableMap<MODE, MapData>, dataType: DataType): Boolean
    fun updateSingleMapData(uid: Int, mapData: MapData, dataType: DataType): Boolean
    fun loadUserAdventureModeController(uid: Int): UserAdventureMode?
    fun updateUserAdventureMode(
        uid: Int,
        userAdventureMode: UserAdventureMode
    ): Boolean

    fun loadUserBlockReward(uid: Int): MutableMap<BLOCK_REWARD_TYPE, MutableMap<DataType, UserBlockReward>>

    fun subUserBlockReward(
        uid: Int,
        dataType: DataType,
        rewardType: BLOCK_REWARD_TYPE,
        value: Float,
        reason: String
    )

    fun updateBomberEnergyAndStage(uid: Int, dataType: DataType, bombermans: List<Hero>): Boolean
    fun updateBombermanActive(
        uid: Int,
        dataType: DataType,
        bomberId: Int,
        active: Boolean,
        stage: Int,
        type: Int,
        energy: Int
    ): Boolean

    fun loadUserHouse(dataType: DataType, uid: Int, limit: Int, offset: Int): Map<Int, House>
    fun loadHeroInHouseRent(dataType: DataType, uid: Int): Map<Int, Int>
    fun getAllHouseOldSeason(uid: Int, dataType: DataType): List<House>
    fun updateUserHouseStage(dataType: DataType, uid: Int, houses: List<House>)
    fun updateBombermanStage(uid: Int, bombermanList: List<Hero>, energiesRecovery: Map<Int, Int>): Boolean
    fun updateBombermanName(name: String, bomberId: Int, userId: Int): Boolean
    fun deleteHouseNotExist(dataType: DataType, uid: Int, houseIds: List<Int>)
    fun insertNewHouse(dataType: DataType, uid: Int, house: House): Boolean

    @Throws(SQLException::class)
    fun subUserDepositBcoin(
        uid: Int,
        dataType: DataType,
        depositRewardType: BLOCK_REWARD_TYPE,
        depositRewardAmount: Float,
        rewardType: BLOCK_REWARD_TYPE,
        rewardAmount: Float,
        reason: String
    )

    fun queryPvP(userId: Int): PvPData
    fun queryPvPHeroEnergy(userId: Int): List<PvPHeroEnergyData>
    fun updatePvPHeroEnergy(userId: Int, heroes: String): Boolean
    fun queryUserRank(userId: Int): Int


    @Throws(SQLException::class)
    fun updateUserReward(
        uid: Int,
        dataType: DataType,
        listReward: Map<BLOCK_REWARD_TYPE, Float>?,
        otherWrappers: List<Pair<String, Array<Any?>>>?,
        reason: String
    )

    fun updateUserRankRewardClaim(userId: Int, season: Int, isClaim: Int): Pair<String, Array<Any?>>
    fun loadAllDisableFeatureConfigs(): Map<Int, IntArray>

    fun getHeroTraditional(
        uid: Int,
        configHeroTraditionalManager: IConfigHeroTraditionalManager
    ): ISFSArray

    fun getOfflineRewardConfigs(): Map<Int, String>
    fun getOfflineRewardTHModeConfigs(): MutableMap<DataType, OfflineRewardTHModeConfig>
    fun updateBomberStakeAmount(
        dataType: DataType,
        bomberId: Int,
        type: Int,
        stakeBcoin: Double,
        stakeSen: Double
    ): Boolean

    fun addShieldToBomber(dataType: DataType, bomberId: Int, type: Int, shield: String): Boolean
    fun getShieldHeroFromDatabase(dataType: DataType, bomberId: Int, type: Int): String
    fun logCreateRock(
        uid: Int,
        tx: String,
        listHeroId: List<Int>,
        amount: Float,
        network: DataType,
        status: String
    ): Boolean

    fun checkValidCreateRock(uid: Int, tx: String, network: DataType): Boolean
    fun updateStatusCreateRock(uid: Int, tx: String, network: DataType, status: String): Boolean
    fun logSwapGem(uid: Int, tokenSwap: BLOCK_REWARD_TYPE, amount: Float, unitPrice: Float, network: DataType): Boolean
    fun checkValidSwapGem(uid: Int, timeSwapConfig: Int): Boolean
    fun updateRemainingTotalSwap(remainingTotal: Float): Boolean
    fun getHeroFiFromDatabase(dataType: DataType, bomberId: List<Int>, type: Int): ISFSArray
    fun getNextIdHouse(): Int
    fun getHouseOldSeason(uid: Int, houseId: Int, dataType: DataType): House?
    fun reactiveHouseOldSeason(uid: Int, houseId: Int, dataType: DataType)
    fun rentHouse(uid: Int, houseId: Int, dataType: DataType, endTime: Instant)
    fun getUserQuantityHeroes(uid: Int, heroType: HeroType, dataType: DataType): Int
    fun heroRestHouseRent(heroId: Int, houseId: Int)
    fun heroGoWorkFromHouseRent(heroId: Int, houseId: Int)
}