package com.senspark.game.handler.rock

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.nativeRate.INativeRateManager
import com.senspark.game.data.manager.nativeRate.nativePriceEntry
import com.senspark.game.data.manager.nativeRate.pricesArray
import com.senspark.game.data.manager.rock.IBuyRockManager
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

// V2's sen_price / bcoin_price are replaced by prices[], not kept alongside it: two ways to read one
// number is what pushes the client back into branching per coin.
class GetRockConfigPackV3Handler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_ROCK_PACK_CONFIG_V3

    private val nativeRateManager: INativeRateManager = services.get<INativeRateManager>()

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val buyRockManager = controller.svServices.get<IBuyRockManager>()
            val rockPackConfig = buyRockManager.getListPackage()
            val result = SFSArray()

            rockPackConfig.forEach {
                val prices = pricesArray(
                    BLOCK_REWARD_TYPE.BCOIN to it.getBcoinPrice(),
                    BLOCK_REWARD_TYPE.SENSPARK to it.getSenPrice(),
                )
                nativeRateManager.nativePriceEntry(controller.dataType, it.getBcoinPrice())
                    ?.let(prices::addSFSObject)

                val sfsObject = SFSObject()
                sfsObject.putUtfString("pack_name", it.getName())
                sfsObject.putInt("rock_amount", it.getRockAmount())
                sfsObject.putSFSArray("prices", prices)
                result.addSFSObject(sfsObject)
            }

            val response: ISFSObject = SFSObject()
            response.putSFSArray("data", result)

            sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}
