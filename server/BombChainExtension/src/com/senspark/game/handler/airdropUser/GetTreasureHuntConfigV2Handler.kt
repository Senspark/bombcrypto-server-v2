package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.treassureHunt.ITreasureHuntConfigManager
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class GetTreasureHuntConfigV2Handler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_TH_DATA_CONFIG_V2

    private val treasureHuntDataManager = services.get<ITreasureHuntConfigManager>()

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val config = treasureHuntDataManager.getDataConfig()
            val dataConfig = config.dataConfigs[controller.dataType]!!

            return sendSuccess(controller, requestId, dataConfig)

        } catch (ex: Exception) {
            return sendExceptionError(controller, requestId, ex)
        }
    }
}