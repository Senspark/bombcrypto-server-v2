package com.senspark.game.controller

import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.House
import com.senspark.game.declare.EnumConstants
import com.senspark.game.manager.hero.IUserHeroFiManager
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject

interface IUserHouseManager {
    val activeHouse: House?
    fun getHouse(id: Int): House?
    fun toArray(): List<House>
    fun setActiveHouse(houseId: Int)
    fun addHouse(uHouse: House)
    fun hasHouse(houseId: Int): Boolean
    fun getUserHouses(): Map<Int, House>
    fun removeHouse(houseId: Int)
    fun changeActiveHouse(newHouse: House, oldHouse: House): List<Hero>
    fun initHeroManager(heroManager: IUserHeroFiManager)
    fun buyHouseServer(rarity: Int): House
    fun buyHouseServerWithTokenNetwork(tokenNetwork: EnumConstants.DataType, rarity: Int): House
    fun reactiveHouseOldSeason(houseId: Int)
    fun rentHouse(houseId: Int, numDay: Int): Long
    fun heroRestInHouse(heroId: Int): House?
    fun heroGoWorkFromHouseRent(heroId: Int): House?
    fun getHouseHeroRest(hero: Hero): House?
    fun getHeroInHouse(): ISFSArray
    fun loadMoreHouses(offset: Int, limit: Int)

    /** True when the house is rented from another player (P2P rental), not owned. */
    fun isRentedHouse(houseId: Int): Boolean

    /**
     * Serializes a house for the client, including the P2P rental state.
     * Every place that sends a house list must go through here - the payload was
     * duplicated in three handlers before and drifted apart.
     */
    fun toSfsObject(house: House): ISFSObject

    /** Re-reads the P2P rentals, adding houses just rented and dropping ended ones. */
    fun refreshRentedHouses()

    /**
     * Persists the active flag of the given houses, routing rented ones to
     * house_rental (their user_house row belongs to the owner).
     */
    fun persistHouseStage(houses: List<House>)
}