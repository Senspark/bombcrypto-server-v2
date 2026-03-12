package com.senspark.game.handler.adventure

import com.senspark.common.utils.toSFSArray
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdventureReviveHeroHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.ADVENTURE_REVIVE_HERO_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        coroutine.scope.launch(Dispatchers.Default) {
            try {
                val adsToken = if (data.containsKey("ads_token")) data.getUtfString("ads_token") else null

                val manager = controller.masterUserManager.userAdventureModeManager
                val gemUsed = manager.reviveHero(adsToken)
                val sfsArray = gemUsed.toSFSArray {
                    SFSObject.newInstance().apply {
                        putUtfString("type", it.key)
                        putFloat("value", it.value)
                    }
                }
                val response = SFSObject.newInstance().apply {
                    putInt("hp", manager.matchManager.hero.hp)
                    putSFSArray("gem_used", sfsArray)
                    putInt("revive_count", manager.matchManager.reviveCount)
                }
                sendSuccess(controller, requestId, response)
            } catch (ex: Exception) {
                sendExceptionError(controller, requestId, ex)
            }
        }
    }
}