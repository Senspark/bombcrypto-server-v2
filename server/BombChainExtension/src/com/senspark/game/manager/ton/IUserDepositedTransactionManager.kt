package com.senspark.game.manager.ton

import com.senspark.game.declare.EnumConstants

interface IUserDepositedTransactionManager {
    fun createDepositedTransaction(uid: Int, dataType: EnumConstants.DataType, depositType :EnumConstants.DepositType): Int
    fun updateDepositedTransaction(
        id: Int,
        amount: Double,
        txHash: String,
        token: String,
        dataType: EnumConstants.DataType,
        sender: String? = null
    )
}