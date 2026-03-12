package com.senspark.game.db

import com.senspark.common.service.IGlobalService
import com.senspark.game.data.model.user.UserIAPPack
import com.senspark.game.declare.customTypeAlias.IsToDayWasBought
import com.senspark.game.declare.customTypeAlias.ProductId
import com.senspark.game.declare.customTypeAlias.Quantity

/**
 * Quản lý tất cả các access database về IAP
 */
interface IIapDataAccess : IGlobalService {
    fun clearUserBuyGemTransactionCache(userId: Int)
    fun getUserGemPacksPurchased(userId: Int): MutableMap<ProductId, Quantity>
    fun loadUserSpecialOfferWasBought(uid: Int): Map<ProductId, IsToDayWasBought>
    fun loadUserIapPack(uid: Int): List<UserIAPPack>
    fun saveUserIapPack(uid: Int, packs: List<Pair<ProductId, Long>>)
}

