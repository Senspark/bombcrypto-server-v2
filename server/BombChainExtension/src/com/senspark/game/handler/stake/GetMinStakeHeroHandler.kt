package com.senspark.game.handler.stake

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.stake.IHeroStakeManager
import com.senspark.lib.data.manager.GameConfigManager
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class GetMinStakeHeroHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_MIN_STAKE_HERO_V2
    
    private val _gameConfigManager = services.get<IGameConfigManager>()

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val heroStakeManager = controller.svServices.get<IHeroStakeManager>()
            val minStakeHeroConfig = heroStakeManager.minStakeHeroConfig
            val minStakeBcoin = _gameConfigManager.minStakeBcoinTHV1
            val minStakeSen = _gameConfigManager.minStakeSenTHV1
            val result = SFSArray()

            minStakeHeroConfig.forEach {
                val sfsObject = SFSObject()
                sfsObject.putInt("rarity", it.key)
                //min stake bcoin để từ hero L lên L+
                sfsObject.putInt("min_stake_amount", it.value)
                //min stake bcoin để nhận bcoin TH mode v1
                sfsObject.putInt("min_stake_bcoin", minStakeBcoin[it.key])
                //min stake sen để nhận sen TH mode v1
                sfsObject.putInt("min_stake_sen", minStakeSen[it.key])
                result.addSFSObject(sfsObject)
            }

            val response: ISFSObject = SFSObject()
            response.putSFSArray("data", result)

            return sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            return sendExceptionError(controller, requestId, ex)

        }
    }
}