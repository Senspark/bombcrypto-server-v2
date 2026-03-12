package com.senspark.game.handler.vic

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class DepositVicHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_INVOICE_DEPOSIT_VIC

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (controller.dataType != EnumConstants.DataType.VIC) {
            return sendError(controller, requestId, ErrorCode.NOT_USER_VIC, null)
        }

        return try {
            // VIC only supports VIC_DEPOSIT (no BCOIN_DEPOSIT)
            val depositType = EnumConstants.DepositType.VIC_DEPOSIT
            val invoicePrefix = "DEP"

            val id = controller.masterUserManager.userDepositedTransactionManager.createDepositedTransaction(
                controller.userId, controller.dataType, depositType
            )
            val response = SFSObject()
            val invoice = "$invoicePrefix$id"
            response.putUtfString("invoice", invoice)

            sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}

