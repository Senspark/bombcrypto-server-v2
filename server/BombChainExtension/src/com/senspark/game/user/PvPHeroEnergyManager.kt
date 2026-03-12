package com.senspark.game.user

import com.smartfoxserver.v2.entities.data.ISFSArray

interface IPvPHeroEnergyManager {
    fun getItem(id: Long): PvPHeroEnergy
    fun setItems(items: List<PvPHeroEnergy>)
    fun toJson(): String
    fun toSFSArray(): ISFSArray
}