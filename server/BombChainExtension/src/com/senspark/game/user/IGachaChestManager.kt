package com.senspark.game.user

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.config.IGachaChest
import com.senspark.game.declare.customEnum.GachaChestType

interface IGachaChestManager : IServerService {
    val chestList: List<IGachaChest>
    fun getChest(chestType: GachaChestType): IGachaChest
    fun getChestShop(chestType: GachaChestType): IGachaChest
}