package com.senspark.game.api

import com.senspark.game.declare.EnumConstants.DataType

interface IHeroDatabase {
    fun query(uid: Int, wallet: String, dataType: DataType): List<BlockchainHeroResponse>
    fun queryV3(uid: Int, wallet: String, dataType: DataType) : Boolean
}