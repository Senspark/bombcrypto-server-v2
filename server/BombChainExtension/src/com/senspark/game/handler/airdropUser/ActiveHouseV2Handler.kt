package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserHouseManager
import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.House
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.KickReason
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class ActiveHouseV2Handler : BaseEncryptRequestHandler() {
    override val serverCommand: String = SFSCommand.ACTIVE_HOUSE_V2

    private val factoryDataAccess = services.get<IDataAccessManager>()

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.checkHash()) {
            controller.disconnect(KickReason.CHEAT_LOGIN)
            return
        }

        val houseId = data.getInt("house_id")

        val uHouseController: IUserHouseManager = controller.masterUserManager.houseManager
        val newHouse: House = uHouseController.getHouse(houseId) ?: return sendError(
            controller,
            requestId,
            ErrorCode.HOUSE_NOT_EXIST,
            null
        )

        val houseGenId = newHouse.details.details
        val oldHouseActive: House? = uHouseController.activeHouse
        //TH chua co nha nao active
        if (oldHouseActive == null) {
            newHouse.isActive = true
            val arr = ArrayList<House>()
            arr.add(newHouse)
            factoryDataAccess.gameDataAccess.updateUserHouseStage(controller.dataType, controller.userId, arr)
            return sendUpdateActiveHouse(controller, requestId, houseGenId, ArrayList())
        }

        //th bam active nha dang active
        if (oldHouseActive.houseId == newHouse.houseId) {
            val lstBbm = controller.masterUserManager.heroFiManager.housingHeroes
            return sendUpdateActiveHouse(controller, requestId, houseGenId, lstBbm)
        }

        //TH change nha active
        val bomberMan = uHouseController.changeActiveHouse(newHouse, oldHouseActive)
        return sendUpdateActiveHouse(controller, requestId, newHouse.details.details, bomberMan)
    }

    private fun sendUpdateActiveHouse(
        uController: IUserController,
        requestId: Int,
        houseGenId: String,
        bombers: List<Hero>
    ) {
        val response = SFSObject()
        //house_gen_id_active
        response.putUtfString(SFSField.House_Gen_Id, houseGenId)
        //list bomber thay doi
        val arrBomer = SFSArray()
        response.putSFSArray(SFSField.Bombers, arrBomer)
        val logBuild = StringBuilder()
        logBuild.append("---stage= ")
        for (hero in bombers) {
            val objBbm = SFSObject()
            objBbm.putLong(SFSField.ID, hero.heroId.toLong())
            objBbm.putInt(SFSField.Energy, hero.energy)
            objBbm.putInt(SFSField.Stage, hero.stage)
            objBbm.putInt(SFSField.HeroType, hero.type.value)
            logBuild.append("${hero.stage} ")
            arrBomer.addSFSObject(objBbm)
        }
        logBuild.append("\n$response")
        uController.logger.log(logBuild.toString())
        return sendSuccess(uController, requestId, response)
    }
}