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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SyncHouseV4Handler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.SYNC_HOUSE_V4

    val gameDataAccess = services.get<IGameDataAccess>()

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (controller.isAirdropUser()) {
            val data = syncHouseAirdrop(controller)
            return sendSuccess(controller, requestId, data)
        }

        val database: IHouseDatabase = controller.svServices.get<IBlockchainDatabaseManager>().houseDatabase
        // Fire-and-forget: result arrives later via Redis stream AP_BL_SYNC_HOUSE → SYNC_HOUSE_RESPONSE push.
        val userInfo = controller.userInfo
        val dataType = controller.dataType
        coroutine.scope.launch(Dispatchers.IO) {
            val ok = database.queryV4(userInfo, dataType)
            controller.logger.log("[SYNC_HOUSE_V4] call api with result = $ok")
        }

        val houseController = controller.masterUserManager.houseManager
        val houses = houseController.toArray()
        val sfsHouses = SFSArray()
        for (house in houses) {
            sfsHouses.addSFSObject(houseController.toSfsObject(house))
        }
        val dataToSend = SFSObject()
        dataToSend.putSFSArray(SFSField.Houses, sfsHouses)
        dataToSend.putSFSArray(SFSField.NewHouses, SFSArray())

        sendSuccess(controller, requestId, dataToSend)
    }

    private fun syncHouseAirdrop(controller: IUserController): ISFSObject {
        val houseController: IUserHouseManager = controller.masterUserManager.houseManager
        val houses: List<House> = houseController.toArray()

        val sfsHouses = SFSArray()
        for (house in houses) {
            sfsHouses.addSFSObject(houseController.toSfsObject(house))
        }

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
