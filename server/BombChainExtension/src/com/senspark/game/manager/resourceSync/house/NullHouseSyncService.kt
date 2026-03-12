package com.senspark.game.manager.resourceSync.house

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.hero.IHeroBuilder
import com.senspark.game.data.model.nft.HouseDetails
import com.senspark.game.declare.EnumConstants
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSObject

class NullHouseSyncService : IHouseSyncService {
    override fun syncHouses(
        userController: IUserController,
        syncedDetails: List<HouseDetails>
    ): ISFSObject {
        throw CustomException("Sync feature not support airdrop user")
    }

    override fun syncHousesOffline(
        uid: Int,
        dataType: EnumConstants.DataType,
        syncedDetails: List<HouseDetails>,
        heroBuilder: IHeroBuilder
    ) {
        throw CustomException("Sync feature not support airdrop user")
    }
}