package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserHouseManager
import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.House
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.declare.*
import com.senspark.game.declare.GameConstants.BOMBER_STAGE
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.hero.IUserHeroFiManager
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class ActiveBomberV2Handler : BaseEncryptRequestHandler() {
    override val serverCommand: String = SFSCommand.ACTIVE_BOMBER_V2

    private val factoryDataAccess = services.get<IDataAccessManager>()
    private val gameConfigManager = services.get<IGameConfigManager>()

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.checkHash()) {
            controller.disconnect(KickReason.CHEAT_LOGIN)
            return
        }

        val heroId = data.getInt("id")
        val isActive = data.getInt("active")

        val bbmController: IUserHeroFiManager = controller.masterUserManager.heroFiManager
        val uHouseController: IUserHouseManager = controller.masterUserManager.houseManager
        val hero: Hero = bbmController.getHero(heroId, controller.dataType)
            ?: return sendError(controller, requestId, ErrorCode.BOMBERMAN_NULL, null)

        val wantToActive = isActive == 1
        if (hero.isActive == wantToActive) {
            return responseToClient(controller, requestId, hero)
        }

        if (wantToActive) {
            val curActive = bbmController.activeHeroCount
            val maxActive = gameConfigManager.maxBomberActive
            if (curActive >= maxActive) {
                return sendError(controller, requestId, ErrorCode.BOMBERMAN_MAX_ACTIVE, null)
            }
            if (hero.isLocked) {
                return sendError(controller, requestId, ErrorCode.HERO_FI_IS_LOCKED, null)
            }
        } else {
            val minuteRest = bbmController.getMinuteRest(hero).toLong()
            var uHouse: House? = null
            if (hero.stage == BOMBER_STAGE.HOUSE) {
                uHouse = uHouseController.activeHouse
            }
            val energyRecovery = bbmController.getEnergyIncrease(hero, minuteRest, uHouse)
            hero.addEnergy(energyRecovery)
        }

        hero.isActive = wantToActive
        hero.stage = BOMBER_STAGE.SLEEP
        hero.timeRest = System.currentTimeMillis()

        factoryDataAccess.gameDataAccess.updateBombermanActive(
            controller.userId,
            controller.dataType,
            hero.heroId,
            hero.isActive,
            hero.stage, hero.type.value,
            hero.energy
        )

        return responseToClient(controller, requestId, hero)
    }

    private fun responseToClient(uController: IUserController, requestId: Int, hero: Hero) {
        val response = SFSObject()
        response.putLong(SFSField.ID, hero.heroId.toLong())
        response.putInt(SFSField.Active, if (hero.isActive) 1 else 0)
        response.putInt(SFSField.Energy, hero.energy)
        response.putInt(SFSField.Stage, hero.stage)
        response.putSFSArray(SFSField.Shields, hero.shield.toSFSArray(hero))
        response.putInt(SFSField.HeroType, hero.type.value)
        response.putSFSObject("data", hero.toSFSObject())

        return sendSuccess(uController, requestId, response)
    }
}