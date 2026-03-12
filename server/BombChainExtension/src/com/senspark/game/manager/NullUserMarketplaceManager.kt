package com.senspark.game.manager

import com.senspark.game.api.*
import com.senspark.game.data.model.Filter
import com.senspark.game.data.model.user.UserItem
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class NullUserMarketplaceManager : IUserMarketplaceManager {

    override fun getUserInventory(filter: Filter): UserItem {
        throw CustomException("Feature not support")
    }

    override fun order(orderData: OrderDataRequest): ISFSObject {
        return SFSObject()
    }

    override fun cancelOrder(buyerUid: Int): Boolean {
        return false
    }

    override fun buyV3(buyerUid: Int): BuyDataResponse {
        return BuyDataResponse(
            itemId = 0,
            itemType = 0,
            fixedQuantity = 0,
            totalQuantity = 0,
            expiration = 0
        )
    }

    override fun sellV3(sellData: SellOrEditDataRequest): Boolean {
        return false
    }

    override fun editV3(editData: SellOrEditDataRequest): Boolean {
        return false
    }

    override fun cancelV3(cancelData: CancelDataRequest): Boolean {
        return false
    }

    override fun getMyItemMarket(): ISFSObject {
        return SFSObject()
    }

    override fun getMarketConfig(): ISFSObject {
        return SFSObject()
    }

    override fun getCurrentMinPrice(): ISFSObject {
        return SFSObject()
    }

    override fun getUserInventoryToSFSArray(filter: Filter): ISFSObject {
        return SFSObject()
    }

    override fun getActivity(): ISFSArray {
        return SFSArray()
    }
}
