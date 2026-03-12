package com.senspark.game.handler.heroTR

import com.senspark.common.utils.toSFSArray
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants.HeroTRType
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.user.ITrGameplayManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

open class GetHeroesTraditionalHandler : BaseEncryptRequestHandler() {
    override val serverCommand: String = SFSCommand.GET_HEROES_TRADITIONAL_V3

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val manager = controller.masterUserManager.heroTRManager
            val result =
                when (if (data.containsKey("type")) HeroTRType.valueOf(data.getUtfString("type")) else HeroTRType.HERO) {
                    HeroTRType.HERO -> manager.heroesMapByItemId
                    HeroTRType.SOUL -> manager.heroesSoulMapByItemId
                }
            val response = SFSObject().apply { putSFSArray("data", result.values.toSFSArray { it.toSfsObject() }) }

            // Xoá user này ra khỏi list playing pvp vì đang load hero tr => đang mở main menu
            val trGameplayManager = services.get<ITrGameplayManager>()
            trGameplayManager.leavePvp(controller.userId, controller.dataType)

            sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}