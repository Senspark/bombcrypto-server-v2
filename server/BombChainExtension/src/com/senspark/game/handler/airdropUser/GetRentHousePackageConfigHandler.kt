package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.nativeRate.INativeRateManager
import com.senspark.game.data.manager.nativeRate.nativePriceEntry
import com.senspark.game.data.manager.treassureHunt.IHouseManager
import com.senspark.game.declare.SFSCommand
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class GetRentHousePackageConfigHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_RENT_HOUSE_PACKAGE_CONFIG

    private val nativeRateManager: INativeRateManager = services.get<INativeRateManager>()

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val houseManager = controller.svServices.get<IHouseManager>()
        return try {
            val packages = houseManager.listConfigPackages[controller.dataType]
                ?: throw CustomException("Rent house is not available")
            val result = SFSArray()
            packages.forEach {
                val obj = it.toSFSObject()
                // `price` stays the BCOIN list price; a native-coin network also gets the display price
                nativeRateManager.nativePriceEntry(controller.dataType, it.price.toDouble())?.let { entry ->
                    obj.putSFSArray("prices", SFSArray().apply { addSFSObject(entry) })
                }
                result.addSFSObject(obj)
            }
            val response = SFSObject().apply {
                putSFSArray("data", result)
            }

            sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}
