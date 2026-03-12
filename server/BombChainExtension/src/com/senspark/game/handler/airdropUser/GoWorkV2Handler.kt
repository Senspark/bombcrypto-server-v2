package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.GameConstants.BOMBER_STAGE
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.hero.IUserHeroFiManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GoWorkV2Handler : BaseEncryptRequestHandler() {
    override val serverCommand: String = SFSCommand.GO_WORK_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val id = data.getInt("id")

        val bbmController: IUserHeroFiManager = controller.masterUserManager.heroFiManager
        val bbm: Hero = bbmController.getHero(id, controller.dataType)
            ?: return sendError(controller, requestId, ErrorCode.BOMBERMAN_NULL, null)
        if (!bbm.isActive) {
            controller.logger.error("GoWorkHandler: Bomber man not active = $id ${controller.userName}")
            return sendError(controller, requestId, ErrorCode.BOMBERMAN_ACTIVE_INVALID, null)
        }
        if (bbm.stage == BOMBER_STAGE.WORK) {
            return sendSuccess(controller, requestId, bbm)
        }

        bbmController.setWork(bbm)
        controller.setNeedSave(EnumConstants.SAVE.HERO_STATUS)
        return sendSuccess(controller, requestId, bbm)
    }

    private fun sendSuccess(userController: IUserController, requestId: Int, bbm: Hero) {
        val response = SFSObject()
        response.putLong(SFSField.ID, bbm.heroId.toLong())
        response.putInt(SFSField.Energy, bbm.energy)
        response.putInt(SFSField.HeroType, bbm.type.value)
        return sendSuccess(userController, requestId, response)
    }
}