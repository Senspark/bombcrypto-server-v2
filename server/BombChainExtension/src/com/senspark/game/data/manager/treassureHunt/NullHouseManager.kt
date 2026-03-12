package com.senspark.game.data.manager.treassureHunt

import com.senspark.game.data.model.config.HouseRentPackage
import com.senspark.game.declare.EnumConstants
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray

class NullHouseManager() : IHouseManager {
    override var listConfigPackages: Map<EnumConstants.DataType, List<HouseRentPackage>> = mapOf()
    override fun initialize() {}
    override fun setConfig(listPackage: Map<EnumConstants.DataType, List<HouseRentPackage>>) {}
    override fun packagePrice(dataType: EnumConstants.DataType): ISFSArray {
        return SFSArray()
    }
}