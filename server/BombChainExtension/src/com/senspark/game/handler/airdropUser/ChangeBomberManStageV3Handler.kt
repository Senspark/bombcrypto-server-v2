package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.declare.*
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.hero.IUserHeroFiManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChangeBomberManStageV3Handler : BaseEncryptRequestHandler() {
    override val serverCommand: String = SFSCommand.CHANGE_BBM_STAGE_V3

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val bbmController: IUserHeroFiManager = controller.masterUserManager.heroFiManager

        val newBomberStages = data.getSFSArray("datas")
        val stage = data.getInt("stage")
        

        val bomberMans = ArrayList<Hero>()
        val changeStageWorkHeroes = SFSArray()
        val results = SFSObject()
        var heroType = -1

        for (i in 0 until newBomberStages.size()) {
            val heroId  = newBomberStages.getInt(i)
            val bbm = bbmController.getHero(heroId, controller.dataType)

            // nếu hero không tồn tại hoặc đã inactive thì bỏ qua
            if (bbm == null || !bbm.isActive) {
                break
            }
            when (stage) {
                GameConstants.BOMBER_STAGE.HOUSE -> {
                    if (bbm.stage != GameConstants.BOMBER_STAGE.HOUSE) {
                        bomberMans.add(bbm)
                        bbmController.setGoHouse(bbm) ?: break
                    }
                }

                GameConstants.BOMBER_STAGE.WORK -> {
                    if (bbm.stage != GameConstants.BOMBER_STAGE.WORK) {
                        bomberMans.add(bbm)
                        bbmController.setWork(bbm)
                        val result =
                            controller.masterUserManager.userBlockMapManager.getBombermanDangerousStatus(bbm)
                        heroType =  bbm.type.value
                        changeStageWorkHeroes.addSFSObject(result)
                    }
                }

                else -> {
                    if (bbm.stage != GameConstants.BOMBER_STAGE.SLEEP) {
                        bomberMans.add(bbm)
                        bbmController.setSleep(bbm)
                    }
                }
            }
        }
        controller.setNeedSave(EnumConstants.SAVE.HERO_STATUS)
        if (changeStageWorkHeroes.size() > 0) {
            results.putSFSArray(SFSField.Datas, changeStageWorkHeroes)
            results.putInt(SFSField.HeroType, heroType)
            results.putInt(SFSField.STAGE, GameConstants.BOMBER_STAGE.WORK)

            results.putInt(SFSField.IsDangerous, 0)
            return sendSuccess(controller, requestId, results)
        }
        return sendSuccess(controller, requestId, bomberMans)
    }

    private fun sendSuccess(userController: IUserController, requestId: Int, bbms: List<Hero>) {
        val datas = SFSArray()
        bbms.forEach { bbm ->
            val `object` = SFSObject()
            `object`.putLong(SFSField.ID, bbm.heroId.toLong())
            `object`.putInt(SFSField.Energy, bbm.energy)
            datas.addSFSObject(`object`)
        }

        val response = SFSObject()
        response.putSFSArray(SFSField.Datas, datas)
        response.putInt(SFSField.IsDangerous, 0)
        if(bbms.isNotEmpty()) {
            response.putInt(SFSField.HeroType, bbms[0].type.value)
            response.putInt(SFSField.STAGE, bbms[0].stage)
        }
        
        return sendSuccess(userController, requestId, response)
    }
}
