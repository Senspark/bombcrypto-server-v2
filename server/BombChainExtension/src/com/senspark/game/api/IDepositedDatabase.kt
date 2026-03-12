package com.senspark.game.api

import com.senspark.game.data.model.deposit.UserDeposited
import com.senspark.game.declare.EnumConstants

interface IDepositedDatabase {
    fun query(uid: Int, username: String, dataType: EnumConstants.DataType): UserDeposited
    fun queryV3(uid: Int, username: String, dataType: EnumConstants.DataType)
}