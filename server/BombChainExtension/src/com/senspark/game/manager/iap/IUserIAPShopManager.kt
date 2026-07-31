package com.senspark.game.manager.iap

import com.senspark.game.declare.customEnum.IAPShopType
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject

interface IUserIAPShopManager {
    fun getGemShop(): ISFSArray

    /** [getGemShop] plus a `prices[]` array per pack — the app store owns no price the client can show. */
    fun getGemShopV3(): ISFSArray

    fun buy(
        type: IAPShopType,
        packageName: String,
        productId: String,
        billToken: String,
        transactionId: String,
        storeId: Int,
        isSpecialOffer: Boolean = false
    )

    /**
     * Buy a gem pack with the deposited native coin instead of through the app store. Everything after
     * the payment step is shared with [buy]; only the payment differs. Special offers stay store-only.
     */
    fun buyByNativeToken(productId: String)

    fun getPackShop(): List<ISFSObject>
    fun saveUserIapPack(buyStep: Int = 0)
}