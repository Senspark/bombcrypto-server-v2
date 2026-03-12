package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserHouseManager
import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.House
import com.senspark.game.declare.GameConstants.BOMBER_STAGE
import com.senspark.game.declare.KickReason
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.hero.IUserHeroFiManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class GetActiveBomberV2Handler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_ACTIVE_BOMBER_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.checkHash()) {
            controller.disconnect(KickReason.CHEAT_LOGIN)
            return
        }

        // Chi là get không nhất thiết phải sync
        val bbmController: IUserHeroFiManager = controller.masterUserManager.heroFiManager
        val bbmsActive: List<Hero> = bbmController.activeHeroes
        val uHouseController: IUserHouseManager = controller.masterUserManager.houseManager

        val data = SFSObject()
        val bombersArr = SFSArray()
        data.putSFSArray(SFSField.Bombers, bombersArr)

        for (hero in bbmsActive) {
            val bbmObj = SFSObject()
            bbmObj.putLong(SFSField.ID, hero.heroId.toLong())
            bbmObj.putUtfString(SFSField.GenID, hero.details.details)
            bbmObj.putInt(SFSField.Stage, hero.stage)
            bbmObj.putSFSArray(SFSField.Shields, hero.shield.toSFSArray(hero))
            bbmObj.putInt(SFSField.HeroType, hero.type.value)
            bbmObj.putSFSObject("data", hero.toSFSObject())

            bombersArr.addSFSObject(bbmObj)

            if (hero.stage == BOMBER_STAGE.WORK) {
                bbmObj.putInt(SFSField.Energy, hero.energy)
                continue
            }

            //tinh năng lượng đã hồi phục khi rest hoặc house
            val minuteRest = bbmController.getMinuteRest(hero)
            var uHouse: House? = null
            if (hero.stage == BOMBER_STAGE.HOUSE) {
                uHouse = uHouseController.getHouseHeroRest(hero)
            }
            val energyRecovery = bbmController.getEnergyIncrease(hero, minuteRest.toLong(), uHouse)
            bbmObj.putInt(SFSField.Energy, hero.energy + energyRecovery)
        }

        return sendSuccess(controller, requestId, data)
    }
}