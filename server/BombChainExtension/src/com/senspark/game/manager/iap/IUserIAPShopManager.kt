package com.senspark.game.manager.iap

import com.senspark.game.declare.customEnum.IAPShopType
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject

interface IUserIAPShopManager {
    fun getGemShop(): ISFSArray
    fun buy(
        type: IAPShopType,
        packageName: String,
        productId: String,
        billToken: String,
        transactionId: String,
        storeId: Int,
        isSpecialOffer: Boolean = false
    )

    fun getPackShop(): List<ISFSObject>
    fun saveUserIapPack(buyStep: Int = 0)
}