package com.senspark.game.data.manager.rock

import com.senspark.game.data.model.config.RockPackage
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IShopDataAccess

class BuyRockManager(
    private val _shopDataAccess: IShopDataAccess,
) : IBuyRockManager {
    override var listConfigPackages: MutableList<RockPackage> = mutableListOf()

    override fun initialize() {
        listConfigPackages.addAll(_shopDataAccess.loadRockPackageConfig())
    }

    override fun setConfig(listPackage: List<RockPackage>) {
        listConfigPackages.clear()
        listConfigPackages.addAll(listPackage)
    }

    override fun valueOf(namePackage: String): RockPackage? {
        return listConfigPackages.find { it._name == namePackage }
    }

    override fun getListPackage(): List<RockPackage> {
        return listConfigPackages
    }
}