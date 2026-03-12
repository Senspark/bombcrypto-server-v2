package com.senspark.game.handler.rock

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.rock.IUserRockManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class UpgradeShieldLevelHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.UPGRADE_SHIELD_LEVEL_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            if (controller.userInfo.type != EnumConstants.UserType.FI) {
                throw CustomException("You aren't user FI")
            }

            val heroId = data.getInt("heroId")
            val heroFiManager = controller.masterUserManager.heroFiManager
            val hero = heroFiManager.getHero(heroId, EnumConstants.HeroType.FI)
                ?: throw CustomException("Hero not exist")

            if (!hero.isHeroS && !hero.isFakeS) {
                throw CustomException("Hero $heroId doesn't have shield")
            }

            val userRockManager = controller.svServices.get<IUserRockManager>()
            val (nonce, signature) = userRockManager.upgradeShieldLevel(controller, hero)

            val response: ISFSObject = SFSObject()
            response.putInt("nonce", nonce)
            response.putUtfString("signature", signature)
            response.putSFSArray(SFSField.Rewards, controller.masterUserManager.blockRewardManager.toSfsArrays())
            sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}