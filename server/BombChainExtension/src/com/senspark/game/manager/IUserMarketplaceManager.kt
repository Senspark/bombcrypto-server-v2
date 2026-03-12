package com.senspark.game.manager

import com.senspark.game.api.*
import com.senspark.game.data.model.Filter
import com.senspark.game.data.model.user.UserItem
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject

interface IUserMarketplaceManager {
    fun getUserInventoryToSFSArray(filter: Filter): ISFSObject
    fun getActivity(): ISFSArray
    fun getUserInventory(filter: Filter): UserItem
    fun order(orderData: OrderDataRequest): ISFSObject
    fun cancelOrder(buyerUid: Int): Boolean
    fun buyV3(buyerUid: Int): BuyDataResponse
    fun sellV3(sellData: SellOrEditDataRequest): Boolean
    fun editV3(editData: SellOrEditDataRequest): Boolean
    fun cancelV3(cancelData: CancelDataRequest): Boolean
    
    fun getMyItemMarket(): ISFSObject
    fun getMarketConfig(): ISFSObject
    fun getCurrentMinPrice(): ISFSObject

}