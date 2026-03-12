package com.senspark.game.handler.airdropUser

import com.senspark.game.api.IBlockchainDatabaseManager
import com.senspark.game.api.IHouseDatabase
import com.senspark.game.controller.IUserController
import com.senspark.game.controller.IUserHouseManager
import com.senspark.game.data.model.nft.House
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class SyncHouseV3Handler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.SYNC_HOUSE_V3

    val gameDataAccess = services.get<IGameDataAccess>()

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (controller.isAirdropUser()) {
            val data = syncHouseAirdrop(controller)
            return sendSuccess(controller, requestId, data)
        }
        
        val database: IHouseDatabase = controller.svServices.get<IBlockchainDatabaseManager>().houseDatabase
        val success = try {
            database.queryV3(controller.userInfo, controller.dataType)
        } catch (e: Exception) {
            false
        }
        controller.logger.log("[SYNC_HOUSE_V3] call api with result = $success")
        val houseController = controller.masterUserManager.houseManager
        val houses = houseController.toArray()
        val sfsHouses = SFSArray()
        for (house in houses) {
            val objData = SFSObject()
            objData.putUtfString(SFSField.House_Gen_Id, house.details.details)
            objData.putInt(SFSField.Active, if (house.isActive) 1 else 0)
            sfsHouses.addSFSObject(objData)
        }
        val dataToSend = SFSObject()
        dataToSend.putSFSArray(SFSField.Houses, sfsHouses)
        dataToSend.putSFSArray(SFSField.NewHouses, SFSArray())

        sendSuccess(controller, requestId, dataToSend)
    }

    private fun syncHouseAirdrop(controller: IUserController): ISFSObject {
        val houseController: IUserHouseManager = controller.masterUserManager.houseManager
        val houses: List<House> = houseController.toArray()

        // data house
        val sfsHouses = SFSArray()
        for (house in houses) {
            val objData = SFSObject()
            objData.putUtfString(SFSField.House_Gen_Id, house.details.details)
            objData.putInt(SFSField.Active, if (house.isActive) 1 else 0)
            if (house.endTimeRent != 0L) objData.putLong("end_time_rent", house.endTimeRent)
            sfsHouses.addSFSObject(objData)
        }

        // data house old season
        val oldSeasonHeroes = SFSArray()
        gameDataAccess.getAllHouseOldSeason(controller.userId, controller.dataType)
            .forEach {
                val objData = SFSObject()
                objData.putUtfString(SFSField.House_Gen_Id, it.details.details)
                objData.putInt(SFSField.Active, 0)
                oldSeasonHeroes.addSFSObject(objData)
            }

        val data = SFSObject()
        data.putSFSArray(SFSField.Houses, sfsHouses)
        data.putSFSArray(SFSField.NewHouses, SFSArray())
        data.putSFSArray("old_season", oldSeasonHeroes)
        data.putSFSArray("hero_in_house", houseController.getHeroInHouse())
        return data
    }
}

