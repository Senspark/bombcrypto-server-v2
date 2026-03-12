package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.declare.*
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.hero.IUserHeroFiManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class ChangeBomberManStageV2Handler : BaseEncryptRequestHandler() {
    override val serverCommand: String = SFSCommand.CHANGE_BBM_STAGE_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val bbmController: IUserHeroFiManager = controller.masterUserManager.heroFiManager

        val newBomberStages = data.getSFSArray("datas")

        val bomberMans = ArrayList<Hero>()
        val changeStageWorkHeroes = SFSArray()
        val results = SFSObject()

        for (i in 0 until newBomberStages.size()) {
            val sfsObject = newBomberStages.getSFSObject(i)
            val heroId = sfsObject.getInt("id")
            val stage = sfsObject.getInt("stage")
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
                        result.putInt("stage", GameConstants.BOMBER_STAGE.WORK)
                        result.putInt(SFSField.HeroType, bbm.type.value)
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
            `object`.putInt(SFSField.STAGE, bbm.stage)
            `object`.putInt(SFSField.IsDangerous, 0)
            `object`.putInt(SFSField.HeroType, bbm.type.value)
            datas.addSFSObject(`object`)
        }

        val response = SFSObject()
        response.putSFSArray(SFSField.Datas, datas)
        return sendSuccess(userController, requestId, response)
    }
}