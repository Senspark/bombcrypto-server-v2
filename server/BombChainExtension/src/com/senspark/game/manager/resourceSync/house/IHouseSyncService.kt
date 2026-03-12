package com.senspark.game.manager.resourceSync.house

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.hero.IHeroBuilder
import com.senspark.game.data.model.nft.HouseDetails
import com.senspark.game.declare.EnumConstants
import com.smartfoxserver.v2.entities.data.ISFSObject

interface IHouseSyncService {
    fun syncHouses(userController: IUserController, syncedDetails: List<HouseDetails>): ISFSObject
    fun syncHousesOffline(uid: Int, dataType: EnumConstants.DataType,  syncedDetails: List<HouseDetails>, heroBuilder: IHeroBuilder)
}
