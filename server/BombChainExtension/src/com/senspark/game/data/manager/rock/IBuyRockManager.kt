package com.senspark.game.data.manager.rock

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.config.RockPackage

interface IBuyRockManager : IServerService {
    val listConfigPackages: List<RockPackage>
    fun setConfig(listPackage: List<RockPackage>)
    fun valueOf(namePackage: String): RockPackage?
    fun getListPackage(): List<RockPackage>
}