package com.senspark.game.handler.data

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.IMasterDataManager
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class GetStartGameConfigHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_START_GAME_CONFIG_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val masterDataManager = controller.svServices.get<IMasterDataManager>()
            val masterUserManager = controller.masterUserManager

            val result: ISFSObject = masterDataManager.getGameConfig(1)
            result.apply {
                putBool("no_ads", masterUserManager.userConfigManager.noAds)
                putBool("is_buy_heroes_trial", masterUserManager.heroFiManager.isBuyHeroesTrial())
                putBool("is_get_hero_tr", masterUserManager.heroTRManager.canGetFreeHeroTR())
            }

            sendSuccess(controller, requestId, result)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}