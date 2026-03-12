package com.senspark.game.api

import com.senspark.game.data.model.nft.HouseDetails
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.declare.EnumConstants.DataType

interface IHouseDatabase {
    fun query(userInfo: IUserInfo, dataType: DataType): List<HouseDetails>
    fun queryV3(userInfo: IUserInfo, dataType: DataType): Boolean
}
