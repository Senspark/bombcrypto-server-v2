package com.senspark.game.handler.solana

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class DepositSolHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_INVOICE_DEPOSIT_SOL

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (controller.dataType != EnumConstants.DataType.SOL) {
            return sendError(controller, requestId, ErrorCode.NOT_USER_SOLANA, null)
        }

        return try {
            //Client cũ ko có depositType => mặc định là Sol deposit
            val depositTypeNumber = data.getInt("deposit_type") ?: EnumConstants.DepositType.SOL_DEPOSIT.value
            val depositType = EnumConstants.DepositType.valueOf(depositTypeNumber)
            val isSolDeposit = depositType == EnumConstants.DepositType.SOL_DEPOSIT
            val invoicePrefix = if (isSolDeposit) "DEP" else "BCD"

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