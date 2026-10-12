package com.senspark.game.data.manager.treassureHunt

import com.senspark.game.data.model.config.HouseRentPackage
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.EnumConstants

class HouseManager(
    private val shopDataAccess: IShopDataAccess
) : IHouseManager {
    override var listConfigPackages: Map<EnumConstants.DataType, List<HouseRentPackage>> = mutableMapOf()
        
    override fun initialize() {
        listConfigPackages = shopDataAccess.loadHouseRentPackageConfig()
    }

    override fun setConfig(listPackage: Map<EnumConstants.DataType, List<HouseRentPackage>>) {
        listConfigPackages = listPackage
    }

}