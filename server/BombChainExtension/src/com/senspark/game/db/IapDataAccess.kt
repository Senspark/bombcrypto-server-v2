package com.senspark.game.db

import com.senspark.common.IDatabase
import com.senspark.common.cache.ICacheService
import com.senspark.common.utils.ILogger
import com.senspark.game.constant.CachedKeys
import com.senspark.game.data.model.user.UserIAPPack
import com.senspark.game.declare.customTypeAlias.IsToDayWasBought
import com.senspark.game.declare.customTypeAlias.ProductId
import com.senspark.game.declare.customTypeAlias.Quantity
import com.senspark.game.utils.serialize
import com.senspark.lib.db.BaseDataAccess
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class IapDataAccess(
    database: IDatabase,
    private val log: Boolean,
    logger: ILogger,
) : BaseDataAccess(database, log, logger), IIapDataAccess {

    override fun initialize() {
    }

    override fun clearUserBuyGemTransactionCache(userId: Int) {
        // do nothing
    }

    override fun getUserGemPacksPurchased(userId: Int): MutableMap<ProductId, Quantity> {
        val result = mutableMapOf<ProductId, Quantity>()
        val statement = """
            SELECT product_id,
                   COUNT(*) AS quantity
            FROM user_buy_gem_transaction
            WHERE uid = ?
              AND is_special_offer = FALSE
            GROUP BY product_id;
        """.trimIndent()
        database.createQueryBuilder(log)
            .addStatement(statement, arrayOf(userId))
            .executeQuery {
                result[it.getString("product_id")] = it.getInt("quantity")
            }
        return result
    }

    override fun loadUserSpecialOfferWasBought(uid: Int): Map<ProductId, IsToDayWasBought> {
        val result = mutableMapOf<ProductId, IsToDayWasBought>()
        val statement = """
            WITH _data AS (SELECT product_id, MAX(date)::date AS purchased_date
                           FROM user_buy_gem_transaction
                           WHERE uid = ?
                             AND is_special_offer = FALSE
                           GROUP BY product_id)
            SELECT product_id,
                   purchased_date = CURRENT_DATE AT TIME ZONE 'utc' AS is_today_was_bought
            FROM _data;
        """.trimIndent()
        database.createQueryBuilder(log)
            .addStatement(statement, arrayOf(uid))
            .executeQuery {
                val productId = it.getString("product_id")
                val isToDayWasBought = it.getBoolean("is_today_was_bought")
                result[productId] = isToDayWasBought
            }
        return result
    }

    override fun loadUserIapPack(uid: Int): List<UserIAPPack> {
        val result = mutableListOf<UserIAPPack>()
        val statement = """
            SELECT uid,
                   product_id,
                   EXTRACT(EPOCH FROM sale_end_date) AS sale_end_date
            FROM user_iap_pack
            WHERE uid = ?;
        """.trimIndent()
        database.createQueryBuilder(log)
            .addStatement(statement, arrayOf(uid))
            .executeQuery {
                result.add(UserIAPPack.fromResultSet(it))
            }
        return result
    }

    override fun saveUserIapPack(uid: Int, packs: List<Pair<ProductId, Long>>) {
        if (packs.isEmpty()) {
            return
        }
        val statement = """
            INSERT INTO user_iap_pack(uid, product_id, sale_end_date)
            VALUES ${packs.joinToString(",") { "(?, ?, NOW() AT TIME ZONE 'utc' + MAKE_INTERVAL(secs := ?))" }}
            ON CONFLICT (uid,product_id)
                DO UPDATE SET sale_end_date = excluded.sale_end_date;;
        """.trimIndent()
        val params = packs.flatMap { listOf<Any?>(uid, it.first, it.second) }.toTypedArray()
        database.createQueryBuilder()
            .addStatementUpdate(statement, params)
            .executeUpdate()
    }
}

class CachedIapDataAccess(
    private val _bridge: IIapDataAccess,
    private val _cache: ICacheService
) : IIapDataAccess {

    override fun initialize() {
    }
    
    /**
     * Invalidate cache
     */
    override fun clearUserBuyGemTransactionCache(userId: Int) {
        _cache.deleteFromHash(CachedKeys.IAP_GEM_PURCHASED, userId.toString())
        _cache.deleteFromHash(CachedKeys.IAP_SPECIAL_OFFER_BOUGHT, userId.toString())
    }

    /**
     * Read
     */
    override fun getUserGemPacksPurchased(userId: Int): MutableMap<ProductId, Quantity> {
        val field = userId.toString()
        try {
            return Json.decodeFromString<MutableMap<ProductId, Quantity>>(_cache.getFromHash(CachedKeys.IAP_GEM_PURCHASED, field)!!)
        } catch (e: Exception) {
            val result = _bridge.getUserGemPacksPurchased(userId)
            _cache.setToHash(CachedKeys.IAP_GEM_PURCHASED, field, result.serialize())
            return result
        }
    }

    /**
     * Read
     */
    override fun loadUserSpecialOfferWasBought(uid: Int): Map<ProductId, IsToDayWasBought> {
        val field = uid.toString()
        try {
            return Json.decodeFromString<Map<ProductId, IsToDayWasBought>>(_cache.getFromHash(CachedKeys.IAP_SPECIAL_OFFER_BOUGHT, field)!!)
        } catch (e: Exception) {
            val result = _bridge.loadUserSpecialOfferWasBought(uid)
            _cache.setToHash(CachedKeys.IAP_SPECIAL_OFFER_BOUGHT, field, result.serialize())
            return result
        }
    }

    /**
     * Read
     */
    override fun loadUserIapPack(uid: Int): List<UserIAPPack> {
        val field = uid.toString()
        try {
            return Json.decodeFromString<List<UserIAPPack>>(_cache.getFromHash(CachedKeys.IAP_USER_IAP_PACK_END, field)!!)
        } catch (e: Exception) {
            val result = _bridge.loadUserIapPack(uid)
            _cache.setToHash(CachedKeys.IAP_USER_IAP_PACK_END, field, result.serialize())
            return result
        }
    }

    /**
     * Write (Invalidate cache)
     */
    override fun saveUserIapPack(uid: Int, packs: List<Pair<ProductId, Long>>) {
        _bridge.saveUserIapPack(uid, packs)
        _cache.deleteFromHash(CachedKeys.IAP_USER_IAP_PACK_END, uid.toString())
    }
}