package com.senspark.game.data.manager.rock

import com.senspark.game.data.model.config.RockPackage

class NullBuyRockManager : IBuyRockManager {

    override val listConfigPackages: List<RockPackage> get() = emptyList()

    override fun initialize() {
    }

    override fun setConfig(listPackage: List<RockPackage>) {}

    override fun valueOf(namePackage: String): RockPackage? {
        return null
    }

    override fun getListPackage(): List<RockPackage> {
        return emptyList()
    }
}