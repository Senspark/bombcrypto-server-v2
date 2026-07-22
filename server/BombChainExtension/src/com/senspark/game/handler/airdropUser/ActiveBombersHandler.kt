package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.controller.IUserHouseManager
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.House
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.declare.*
import com.senspark.game.declare.GameConstants.BOMBER_STAGE
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.hero.IUserHeroFiManager
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class ActiveBombersHandler : BaseEncryptRequestHandler() {
    override val serverCommand: String = SFSCommand.ACTIVE_BOMBERS

    private val factoryDataAccess = services.get<IDataAccessManager>()
    private val gameConfigManager = services.get<IGameConfigManager>()

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.checkHash()) {
            controller.disconnect(KickReason.CHEAT_LOGIN)
            return
        }

        val idsArray = data.getSFSArray(SFSField.HERO_IDS)
        val wantToActive = data.getInt(SFSField.Active) == 1

        val bbmController: IUserHeroFiManager = controller.masterUserManager.heroFiManager
        val uHouseController: IUserHouseManager = controller.masterUserManager.houseManager

        if (idsArray == null || idsArray.size() == 0) {
            return sendSuccess(controller, requestId, buildResponse(emptyList()))
        }

        val resolved = ArrayList<Hero>(idsArray.size())
        for (i in 0 until idsArray.size()) {
            val heroId = idsArray.getInt(i)
            val hero = bbmController.getHero(heroId, controller.dataType)
                ?: return sendError(controller, requestId, ErrorCode.BOMBERMAN_NULL, null)
            resolved.add(hero)
        }

        val toChange = resolved.filter { it.isActive != wantToActive }

        if (wantToActive) {
            val curActive = bbmController.activeHeroCount
            val maxActive = gameConfigManager.maxBomberActive
            if (curActive + toChange.size > maxActive) {
                return sendError(controller, requestId, ErrorCode.BOMBERMAN_MAX_ACTIVE, null)
            }
            if (toChange.any { it.isLocked }) {
                return sendError(controller, requestId, ErrorCode.HERO_FI_IS_LOCKED, null)
            }
        }

        if (toChange.isEmpty()) {
            return sendSuccess(controller, requestId, buildResponse(emptyList()))
        }

        val now = System.currentTimeMillis()
        for (hero in toChange) {
            if (!wantToActive) {
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
            hero.timeRest = now

            factoryDataAccess.gameDataAccess.updateBombermanActive(
                controller.userId,
                controller.dataType,
                hero.heroId,
                hero.isActive,
                hero.stage,
                hero.type.value,
                hero.energy
            )
        }

        return sendSuccess(controller, requestId, buildResponse(toChange))
    }

    private fun buildResponse(heroes: List<Hero>): SFSObject {
        val response = SFSObject()
        val bombersArr = SFSArray()
        for (hero in heroes) {
            val obj = SFSObject()
            obj.putLong(SFSField.ID, hero.heroId.toLong())
            obj.putInt(SFSField.Active, if (hero.isActive) 1 else 0)
            obj.putInt(SFSField.Energy, hero.energy)
            obj.putInt(SFSField.Stage, hero.stage)
            obj.putSFSArray(SFSField.Shields, hero.shield.toSFSArray(hero))
            obj.putInt(SFSField.HeroType, hero.type.value)
            obj.putSFSObject(SFSField.Data, hero.toSFSObject())
            bombersArr.addSFSObject(obj)
        }
        response.putSFSArray(SFSField.Bombers, bombersArr)
        return response
    }
}
