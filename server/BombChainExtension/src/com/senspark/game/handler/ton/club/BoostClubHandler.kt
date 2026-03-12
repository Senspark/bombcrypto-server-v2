package com.senspark.game.handler.ton.club

import com.senspark.common.utils.toSFSArray
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.ton.ClubHelper
import com.senspark.game.manager.ton.IClubManager
import com.senspark.lib.data.manager.GameConfigManager
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class BoostClubHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.BOOST_CLUB_V2

    private val _gameConfigManager = services.get<IGameConfigManager>()

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (controller.dataType != EnumConstants.DataType.TON) {
            return sendError(controller, requestId, ErrorCode.NOT_USER_TON, null)
        }

        try {
            val clubManager = controller.svServices.get<IClubManager>()
            var clubId = data.getInt("id")

            //support old Ton client
            val clubIdLong = data.getLong("club_id")
            if (clubIdLong != null && clubIdLong > 0) {
                if (clubIdLong > Int.MAX_VALUE) {
                    return sendError(controller, requestId, ErrorCode.SERVER_ERROR, "Club ID is out of range")
                }
                clubId = clubIdLong.toInt()
            }

            val packageId = data.getInt("package_id")
            clubManager.boostClub(controller, clubId, packageId)

            val bidPrice = clubManager.getBidPackage(clubId)
            val response = SFSObject().apply {
                putSFSArray("data", bidPrice.toSFSArray {
                    SFSObject().apply {
                        putInt("package_id", it.key)
                        // làm tròn 1 chữ số
                        putFloat(
                            "bid_price",
                            ClubHelper.calculateBidPrice(_gameConfigManager.bidUnitPrice, it.value)

                        )
                    }
                })
            }

            return sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            return sendExceptionError(controller, requestId, e)
        }
    }
}