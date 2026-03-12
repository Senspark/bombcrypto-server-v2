package com.senspark.game.manager.iap

import com.senspark.game.declare.customEnum.IAPShopType
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray

class NullUserIAPShopManager : IUserIAPShopManager {
    override fun getGemShop(): ISFSArray {
        return SFSArray()
    }

    override fun saveUserIapPack(buyStep: Int) {}

    override fun getPackShop(): List<ISFSObject> {
        return emptyList()
    }

    override fun buy(
        type: IAPShopType,
        packageName: String,
        productId: String,
        billToken: String,
        transactionId: String,
        storeId: Int,
        isSpecialOffer: Boolean
    ) {}
}