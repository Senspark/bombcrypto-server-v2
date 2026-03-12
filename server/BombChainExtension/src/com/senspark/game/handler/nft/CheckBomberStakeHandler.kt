package com.senspark.game.handler.nft

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants.HeroType
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.KickReason
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.stake.IHeroStakeManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class CheckBomberStakeHandler : BaseEncryptRequestHandler()  {
    override val serverCommand = SFSCommand.CHECK_BOMBER_STAKE_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.checkHash()) {
            controller.disconnect(KickReason.CHEAT_LOGIN)
            return
        }
        try {
            
            val bomberId = data.getLong(SFSField.ID).toInt();
            val heroFiManager = controller.masterUserManager.heroFiManager
            val bomber = heroFiManager.getHero(bomberId, HeroType.FI)
                ?: return sendError(controller, requestId, ErrorCode.BOMBERMAN_NULL, null)

            val heroesStakeManager = controller.svServices.get<IHeroStakeManager>()
            heroesStakeManager.checkBomberStake(controller.dataType, bomber)

            val response: ISFSObject = SFSObject()
            response.putLong(SFSField.ID, bomber.heroId.toLong())
            response.putInt(SFSField.Active, if (bomber.isActive) 1 else 0)
            response.putInt(SFSField.Energy, bomber.energy)
            response.putInt(SFSField.Stage, bomber.stage)
            response.putSFSArray(SFSField.Shields, bomber.shield.toSFSArray(bomber))
            response.putInt(SFSField.HeroType, bomber.type.value)
            response.putDouble(SFSField.StakeBcoin, bomber.stakeBcoin)
            response.putDouble(SFSField.StakeSen, bomber.stakeSen)
            response.putSFSObject("data", bomber.toSFSObject())

            return sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}