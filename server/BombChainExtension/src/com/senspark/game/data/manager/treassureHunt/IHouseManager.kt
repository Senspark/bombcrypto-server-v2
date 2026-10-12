package com.senspark.game.data.manager.treassureHunt

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.config.HouseRentPackage
import com.senspark.game.declare.EnumConstants

interface IHouseManager: IServerService {
    var listConfigPackages: Map<EnumConstants.DataType, List<HouseRentPackage>>
    fun setConfig(listPackage: Map<EnumConstants.DataType, List<HouseRentPackage>>)
}