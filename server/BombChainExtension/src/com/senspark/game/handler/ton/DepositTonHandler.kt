package com.senspark.game.handler.ton

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class DepositTonHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_INVOICE_DEPOSIT_TON_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (controller.dataType != EnumConstants.DataType.TON) {
            return sendError(controller, requestId, ErrorCode.NOT_USER_TON, null)
        }

        return try {

            //Client cũ ko có depositType => mặc định là Ton deposit
            val depositTypeNumber = data.getInt("deposit_type") ?: EnumConstants.DepositType.TON_DEPOSIT.value
            val depositType = EnumConstants.DepositType.valueOf(depositTypeNumber)
            val isTonDeposit = depositType == EnumConstants.DepositType.TON_DEPOSIT
            val invoicePrefix = if (isTonDeposit) "DEP" else "BCD"

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