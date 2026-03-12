package com.senspark.game.db

import com.google.gson.JsonArray
import com.senspark.common.IDatabase
import com.senspark.common.constant.ItemId
import com.senspark.common.utils.ILogger
import com.senspark.game.api.IPvpResultInfo
import com.senspark.game.constant.ItemStatus
import com.senspark.game.constant.ItemType
import com.senspark.game.data.manager.gacha.IGachaChestSlotManager
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.model.Filter
import com.senspark.game.data.model.PvpRankingReward
import com.senspark.game.data.model.auth.IUserLoginInfo
import com.senspark.game.data.model.autoMine.IUserAutoMine
import com.senspark.game.data.model.autoMine.UserAutoMine
import com.senspark.game.data.model.config.*
import com.senspark.game.data.model.deposit.UserDeposited
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.user.*
import com.senspark.game.db.helper.QueryHelper
import com.senspark.game.db.model.UserConfig
import com.senspark.game.db.model.UserFreeRewardConfig
import com.senspark.game.declare.EnumConstants.*
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.UserNameSuffix
import com.senspark.game.declare.customEnum.ChangeRewardReason
import com.senspark.game.declare.customEnum.SubscriptionProduct
import com.senspark.game.declare.customEnum.SubscriptionState
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.config.MiscConfigs
import com.senspark.game.manager.dailyTask.DailyTask
import com.senspark.game.manager.dailyTask.TodayTask
import com.senspark.game.manager.onBoarding.UserProgress
import com.senspark.game.schema.*
import com.senspark.game.utils.deserializeList
import com.senspark.game.utils.serialize
import com.senspark.lib.db.BaseDataAccess
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.intellij.lang.annotations.Language
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.SQLException
import java.sql.Timestamp
import java.time.Instant

class UserDataAccess(
    database: IDatabase,
    private val enableSqlLog: Boolean,
    private val _logger: ILogger,
    private val executor: UserDataAccessExecutor = UserDataAccessExecutor(database, enableSqlLog, _logger),
) : BaseDataAccess(database, enableSqlLog, _logger), IUserDataAccess {

    override fun initialize() {
    }

    override fun getUserInfo(info: IUserLoginInfo, deviceType: DeviceType): IUserInfo {
        val loginColumnName = when (deviceType) {
            DeviceType.WEB -> "lastlogin"
            DeviceType.MOBILE -> "lastlogin_mobile"
            else -> throw Exception("Does not support this device type $deviceType")
        }
        val statement = """
            UPDATE "user"
            SET
                $loginColumnName = NOW() AT TIME ZONE 'utc'
            WHERE id_user = ?
            RETURNING
                *,
                EXTRACT(EPOCH FROM GREATEST(lastlogout, lastlogout_mobile))::BIGINT AS original_last_logout,
                EXTRACT(EPOCH FROM datecreate)::BIGINT AS datecreate_mili
        """.trimIndent()
        val params = arrayOf<Any?>(info.userId)
        val sfsArray = executeQueryAndThrowException(statement, params)
        return UserInfo(sfsArray.getSFSObject(0))
    }

    override fun getDisplayNameUser(uid: Int): String {
        val statement = """
            SELECT CASE
                   WHEN name IS NOT NULL THEN name
                   WHEN second_username IS NOT NULL THEN second_username
                   ELSE
                       CASE
                           WHEN LENGTH(user_name) > 10 THEN CONCAT(SUBSTRING(user_name, 0, 6), '...',
                                                                     SUBSTRING(user_name, LENGTH(user_name) - 3, 4))
                           ELSE user_name END
                   END AS name 
            FROM "user"
            WHERE id_user = ?;
            """
        val displayName = mutableListOf<String>()
        executeQueryAndThrowException(statement, arrayOf(uid)) {
            displayName.add(it.getString("name"))
        }
        return displayName[0]
    }

    override fun saveUserLoginInfo(info: IUserLoginInfo, deviceType: DeviceType): IUserInfo {
        val loginColumnName = when (deviceType) {
            DeviceType.WEB -> "lastlogin"
            DeviceType.MOBILE -> "lastlogin_mobile"
            else -> throw Exception("Does not support this device type $deviceType")
        }

        val userNameOnDb = UserNameSuffix.removeSuffixName(info.username)
        val statement = """
            WITH
                _existed_user AS (
                    SELECT COUNT(id_user) AS count
                    FROM "user"
                    WHERE id_user = ?
                )
            INSERT INTO "user" (
                id_user,
                user_name,
                second_username,
                email,
                mode,
                type,
                name,
                datecreate
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id_user) DO UPDATE
            SET
                user_name        = excluded.user_name,
                second_username  = excluded.second_username,
                email            = excluded.email,
                name             = excluded.name,
                $loginColumnName = NOW() AT TIME ZONE 'utc'
            RETURNING
                *,
                EXTRACT(EPOCH FROM GREATEST(lastlogout, lastlogout_mobile))::BIGINT AS original_last_logout,
                EXTRACT(EPOCH FROM datecreate)::BIGINT AS datecreate_mili,
                ((SELECT count FROM _existed_user) = 0)::INT AS new_user;
        """.trimIndent()
        val params = arrayOf<Any?>(
            info.userId,
            info.userId,
            userNameOnDb, // remove suffix name, only get 0x...0
            info.loginUsername,
            info.email,
            UserMode.NON_TRIAL.name,
            info.userType.name,
            info.displayName,
            Timestamp(info.createAt),
        )
        val sfsArray = database.createQueryBuilder(enableSqlLog).addStatement(statement, params).executeQuery()
        return UserInfo(sfsArray.getSFSObject(0))
    }

    override fun changeMiningToken(uid: Int, tokenType: TokenType): Boolean {
        return executor.changeMiningToken(uid, tokenType)
    }

    override fun buyItemMarketplace(
        item: Item,
        quantity: Int,
        unitPrice: Float,
        buyerId: Int,
        rewardType: BLOCK_REWARD_TYPE,
        expirationAfter: Int
    ) {
        val statement = "CALL sp_buy_item_marketplace(?,?,?,?,?,?,?,?)"
        executeUpdateThrowException(
            statement,
            arrayOf(
                item.id,
                item.type.value,
                quantity,
                unitPrice,
                buyerId,
                rewardType.value,
                item.name,
                expirationAfter
            )
        )
    }

    override fun sellItemMarketplace(
        listId: List<Int>,
        type: Int,
        itemId: Int,
        quantity: Int,
        price: Float,
        uid: Int,
        rewardType: Int,
        expirationAfter: Int
    ) {
        val statement = "CALL sp_sell_item_marketplace(?::json, ?, ?, ?, ?, ?, ?, ?)"
        executeUpdateThrowException(
            statement,
            arrayOf(listId.serialize(), type, itemId, quantity, price, uid, rewardType, expirationAfter)
        )
    }

    override fun cancelItemMarketplace(uid: Int, itemId: Int, quantity: Int, unitPrice: Float, expirationAfter: Int) {
        val statement = "CALL sp_cancel_item_marketplace(?, ?, ?, ?, ?)"
        database.createQueryBuilder(enableSqlLog)
            .addStatement(statement, arrayOf(uid, itemId, quantity, unitPrice, expirationAfter))
            .executeUpdate()
    }

    override fun editItemMarketplace(
        uid: Int,
        itemId: Int,
        oldQuantity: Int,
        oldUnitPrice: Float,
        newQuantity: Int,
        newUnitPrice: Float,
        expirationAfter: Int
    ) {
        val statement = """CALL sp_edit_item_marketplace(?,?,?,?,?,?,?);"""
        database.createQueryBuilder(enableSqlLog)
            .addStatement(
                statement,
                arrayOf(uid, itemId, oldQuantity, oldUnitPrice, newQuantity, newUnitPrice, expirationAfter)
            )
            .executeUpdate()
    }

    override fun changeDefaultMiningToken(uid: Int, tokenType: TokenType): Boolean {
        return executor.changeDefaultMiningToken(uid, tokenType)
    }

    override fun loadUserStakeVip(username: String): List<UserStakeVipReward> {
        val statement = """
            WITH _user_stake AS (SELECT amount,
                                        vip_date
                                 FROM user_stake
                                 WHERE username = ?),
                 _rewards AS (SELECT *
                              FROM config_stake_vip_reward
                              WHERE stake_amount = (SELECT MAX(DISTINCT (stake_amount))
                                                    FROM config_stake_vip_reward
                                                    WHERE stake_amount <= (SELECT amount FROM _user_stake))),
                 _reward_claimed AS (SELECT *
                                     FROM user_stake_vip_claimed
                                     WHERE username = ?),
                 _new_rewards AS (SELECT r.level,
                                         r.reward_type,
                                         r.type,
                                         r.quantity,
                                         r.dates,
                                         CASE
                                             WHEN rc.claim_date IS NULL OR (v.vip_date::timestamp > rc.claim_date::timestamp)::bool
                                                 THEN v.vip_date
                                             ELSE rc.claim_date
                                             END AS last_claim
                                  FROM _rewards AS r
                                           LEFT JOIN _reward_claimed AS rc
                                                     ON r.reward_type = rc.reward_type AND r.type = rc.type,
                                       (SELECT vip_date FROM _user_stake) AS v)
            SELECT nr.level,
                   nr.reward_type,
                   nr.type,
                   nr.quantity,
                   CASE
                       WHEN DIV(EXTRACT(DAY FROM CURRENT_TIMESTAMP::timestamp - nr.last_claim::timestamp)::numeric, nr.dates) > 0
                           THEN quantity
                       ELSE 0
                       END                                       AS having_quantity,
                   nr.last_claim + (nr.dates || 'DAY')::interval AS next_claim
            FROM _new_rewards AS nr;
        """.trimIndent()
        return executor.loadUserStakeVip(statement, username)
    }

    override fun claimStakeVipReward(uid: Int, dataType: DataType, userStakeVipReward: UserStakeVipReward) {
        val statement1: String
        val params1: Array<Any?>
        when (userStakeVipReward.rewardType) {
            StakeVipRewardType.REWARD -> {
                val pair = QueryHelper.queryAddUpsertUserBlockReward(
                    uid,
                    BLOCK_REWARD_TYPE.valueOf(userStakeVipReward.type),
                    dataType,
                    userStakeVipReward.havingQuantity.toFloat(),
                    ChangeRewardReason.CLAIM_STAKE_VIP_REWARD
                )
                statement1 = pair.first
                params1 = pair.second
            }

            StakeVipRewardType.BOOSTER -> {
                throw CustomException("Not implement yet")
            }
        }
        // Update thời gian claim
        val statement2 = """
            INSERT INTO user_stake_vip_claimed(username, reward_type, type, claim_date)
            VALUES ((SELECT user_name FROM "user" WHERE id_user = ?),
                    ?,
                    ?,
                    CURRENT_TIMESTAMP(0))
            ON CONFLICT (username, reward_type, type)
                DO UPDATE SET claim_date = CURRENT_TIMESTAMP(0);
        """.trimIndent()
        val params2 = arrayOf<Any?>(uid, userStakeVipReward.rewardType.name, userStakeVipReward.type)
        executor.claimStakeVipReward(statement1, params1, statement2, params2)
    }

    override fun subUserPvpBoosters(uid: Int, boosterIds: List<Int>) {
        if (boosterIds.isEmpty()) {
            return
        }
        val ids = boosterIds.joinToString(",")
        val statement = """
            DELETE
            FROM user_booster
            WHERE uid = ?
              AND id IN ($ids)
        """.trimIndent()
        executeUpdateThrowException(statement, arrayOf(uid))
    }

    override fun activateCode(uid: Int, code: String): Boolean {
        val statement = "SELECT fn_activation_code(?,?) AS activated"
        return executor.activateCode(statement, uid, code)
    }

    override fun isSummaryStoryHunterSeason(): Boolean {
        val statement = "CALL summary_story_hunter_season()"
        val sfsArray = executeUpdateReturnDataThrowException(statement, arrayOf())
        if (sfsArray.size() > 0) {
            val sfsObject = sfsArray.getSFSObject(0)
            return sfsObject.getInt("result_success") == 1
        }
        return false
    }


    override fun summaryPvpRankingReward() {
        val statement = "CALL summary_pvp_ranking_reward()"
        executeUpdateThrowException(statement, arrayOf())
    }

    override fun isLoadDataManager(schedule: String, season: Int): Boolean {
        return transaction {
            TableScheduleStatus.slice(TableScheduleStatus.schedule).select(
                (TableScheduleStatus.schedule eq schedule)
                        and (TableScheduleStatus.season eq season)
                        and (TableScheduleStatus.status eq 0)
            ).count() == 1L
        }
    }

    override fun getConfigPvpRankingReward(): List<PvpRankingReward> {
        val result = mutableListOf<PvpRankingReward>()
        val statement = """
            SELECT rank_min, rank_max, reward
            FROM config_pvp_ranking_reward
            ORDER BY rank_min ASC
        """.trimIndent()
        executeQuery(statement, arrayOf()) {
            result.add(PvpRankingReward.fromResultSet(it))
        }
        return result
    }


    override fun getPvpRankingReward(
        pvpMatchReward: Int,
        season: Int
    ): Map<Int, UserPvpRankingReward> {
        if (season <= 1) return HashMap()
        val statement = """
            SELECT "uid"    AS uid,
                   "rank"        AS rank_number,
                   is_claim      AS is_claim,
                   reward        AS reward,         
                   total_match   AS total_match
            FROM user_pvp_rank_reward_ss_${season}
        """.trimIndent()
        val userPvpRankingRewards: MutableMap<Int, UserPvpRankingReward> = HashMap()
        executeQuery(statement, arrayOf()) {
            val userId = it.getInt("uid")
            val userPvpRankingReward = UserPvpRankingReward.fromResultSet(it, pvpMatchReward)
            userPvpRankingRewards[userId] = userPvpRankingReward
        }
        return userPvpRankingRewards
    }

    override fun logPvpBooster(type: String, itemId: Int, boosterName: String, userId: Int, boosterPrice: Int) {
        transaction {
            TableLogPvpBooster.insert {
                it[date] = Instant.now()
                it[uid] = userId
                it[TableLogPvpBooster.type] = type
                it[TableLogPvpBooster.itemId] = itemId
                it[TableLogPvpBooster.boosterName] = boosterName
                it[feeAmount] = boosterPrice.toFloat()
            }
        }
    }

    override fun loadUserPvpBoosters(uid: Int): Map<Int, UserBooster> {
        val results: MutableMap<Int, UserBooster> = HashMap()
        val statement =
            """
                SELECT COUNT(user_booster.item_id)::INT4                        as quantity,
                       user_booster.item_id,
                       jsonb_agg(user_booster.id order by uis.status ASC)::text as list_id
                FROM user_booster
                         INNER JOIN user_item_status uis on user_booster.item_id = uis.item_id AND user_booster.id = uis.id
                    AND uis.status <> 2
                WHERE user_booster.uid = ?
                GROUP BY user_booster.item_id
            """.trimIndent()
        val params = arrayOf<Any?>(uid)
        executeQuery(statement, params) {
            val itemId = it.getInt("item_id")
            results[itemId] = UserBooster.fromResultSet(it)
        }
        return results
    }

    override fun hasClaimReward(userId: Int, season: Int): Boolean {
        val statement = "SELECT is_claim FROM user_pvp_rank_reward_ss_$season WHERE uid = ?"
        val params = arrayOf<Any?>(userId)
        val list = mutableListOf<Boolean>()
        executeQuery(statement, params) {
            list.add(it.getInt("is_claim") == 0)
        }
        return if (list.isNotEmpty()) {
            list[0]
        } else {
            true
        }
    }

    override fun getNextRoundStoryHunter(userName: String): Int {
        val statement = """
                SELECT round FROM log_play_boss_hunter WHERE address = ? ORDER BY round DESC LIMIT 1
            """.trimIndent()
        val results = mutableListOf<Int>()
        executeQuery(statement, arrayOf(userName)) {
            results.add(it.getInt("round") + 1)
        }
        if (results.isNotEmpty()) {
            return results[0]
        }
        return 1
    }

    override fun updateAccountName(
        dataType: DataType,
        username: String,
        newName: String,
        fee: Float,
        deposit: Float,
        feeRewardType: BLOCK_REWARD_TYPE
    ) {

//        trừ reward
        val statement1 = """
            SELECT fn_sub_user_reward(
                    (SELECT id_user FROM "user" WHERE user_name = ?),
                    ?,?,?,?,?);
        """.trimIndent()
        val params1 =
            arrayOf<Any?>(
                username,
                dataType.name,
                fee,
                feeRewardType.name,
                feeRewardType.swapDepositedOrReward().name,
                "Update account name"
            )

//        update name
        val statement2 = """
            UPDATE "user"
            SET name = ?
            WHERE user_name = ?
        """.trimIndent()
        val params2 = arrayOf<Any?>(newName, username)
//        log
        val statement3 = """
            INSERT INTO log_user_rename(username, date, reward_type, reward_amount, deposit_amount)
            VALUES (?, NOW() AT TIME ZONE 'utc', ?, ?, ?)
        """.trimIndent()
        val params3 = arrayOf<Any?>(username, feeRewardType.name, fee, deposit)
        database.createQueryBuilder(enableSqlLog)
            .addStatement(statement1, params1)
            .addStatementUpdate(statement2, params2)
            .addStatementUpdate(statement3, params3)
            .executeMultiQuery()
    }

    override fun getStoryHunterBuyTicket(season: Int): Int {
        return 0
    }

    override fun logRepairShield(username: String, dataType: DataType, repairedBomberman: Map<Int, Int>) {
        if (repairedBomberman.isEmpty()) {
            return
        }
        val subStatement = repairedBomberman.map {
            """('$username', CURRENT_TIMESTAMP AT TIME ZONE 'utc', ${it.key}, ${it.value}, '${dataType.name}')"""
        }.joinToString(",\n")
        val statement = """
            INSERT INTO log_repair_shield(username, repair_time, bomber_id, remain_shield, type)
            VALUES $subStatement
        """.trimIndent()
        executor.logRepairShield(statement)
    }

    override fun insertLogRewardDeposit(userName: String, rewardType: String, rewardAmount: Float) {
        transaction {
            TableLogRewardDeposit.insert {
                it[TableLogRewardDeposit.userName] = userName
                it[TableLogRewardDeposit.rewardType] = rewardType
                it[TableLogRewardDeposit.rewardAmount] = rewardAmount
            }
        }
    }

    override fun getUserInventory(
        uid: Int,
        filter: Filter,
        status: Int
    ): MutableMap<ItemId, MutableList<UserItem>> {
        val resultList = mutableMapOf<Int, MutableList<UserItem>>()
        transaction {
            val result = TableUserSkin
                .join(
                    TableUserItemStatus,
                    JoinType.LEFT,
                    additionalConstraint = {
                        (TableUserSkin.uid eq TableUserItemStatus.uid)
                            .and(TableUserSkin.itemId eq TableUserItemStatus.itemId)
                            .and(TableUserSkin.id eq TableUserItemStatus.id)
                    })
                .slice(
                    TableUserSkin.id,
                    TableUserSkin.type,
                    TableUserSkin.itemId,
                    TableUserItemStatus.status,
                    TableUserSkin.status,
                    TableUserSkin.createDate,
                    TableUserSkin.expirationAfter,
                    TableUserItemStatus.expiryDate,
                )
                .select {
                    (TableUserSkin.uid eq uid)
                        .and(TableUserSkin.status neq 2)
                        .and(
                            TableUserItemStatus.expiryDate.isNull()
                                .or(
                                    TableUserItemStatus.status.isNotNull()
                                        .and(TableUserItemStatus.expiryDate greaterEq Instant.now())
                                )
                        )
                }
            if (filter.id != -1) {
                result.andWhere { TableUserSkin.id eq filter.id }
            }
            if (filter.type != -1) {
                result.andWhere { TableUserSkin.type eq filter.type }
            }
            if (status == ItemStatus.Sell.value) {
                result.andWhere { TableUserItemStatus.status neq ItemStatus.Sell.value }
            }
            result.forEach {
                val userItem = UserItem(
                    it[TableUserSkin.id],
                    ItemType.fromValue(it[TableUserSkin.type]),
                    it[TableUserSkin.itemId],
                    if (status == ItemStatus.LockedOrEquipSkin.value) it[TableUserItemStatus.status] else it[TableUserSkin.status],
                    it[TableUserSkin.status],
                    it[TableUserSkin.createDate].toEpochMilli(),
                    it[TableUserItemStatus.expiryDate]?.toEpochMilli(),
                    expirationAfter = it[TableUserSkin.expirationAfter],
                    itemInstantIds = listOf(it[TableUserSkin.id])
                )
                val items = resultList[userItem.itemId] ?: mutableListOf()
                items.add(userItem)
                resultList[userItem.itemId] = items
            }
        }
        return resultList
    }

    override fun getUserBooster(uid: Int, filter: Filter): Map<Int, List<UserItem>> {
        val resultList = mutableMapOf<Int, MutableList<UserItem>>()
        transaction {
            val result = TableUserBooster
                .join(
                    TableUserItemStatus,
                    JoinType.INNER,
                    additionalConstraint = {
                        (TableUserBooster.uid eq TableUserItemStatus.uid)
                            .and(TableUserBooster.itemId eq TableUserItemStatus.itemId)
                            .and(TableUserBooster.id eq TableUserItemStatus.id)
                    })
                .slice(
                    TableUserBooster.id,
                    TableUserBooster.type,
                    TableUserItemStatus.status,
                    TableUserBooster.itemId,
                    TableUserBooster.create,
                    TableUserBooster.status
                )
                .select {
                    (TableUserBooster.uid eq uid)
                        .and(TableUserItemStatus.status neq 2)
                }
            if (filter.id != -1) {
                result.andWhere { TableUserBooster.id eq filter.id }
            }
            if (filter.type != -1) {
                result.andWhere { TableUserBooster.type eq filter.type }
            }
            result.forEach {
                val userItem = UserItem(
                    it[TableUserBooster.id],
                    ItemType.fromValue(it[TableUserBooster.type]),
                    it[TableUserBooster.itemId],
                    it[TableUserItemStatus.status],
                    it[TableUserItemStatus.status],
                    it[TableUserBooster.create].toEpochMilli(),
                    null,
                    expirationAfter = 0,
                    itemInstantIds = listOf(it[TableUserBooster.id])
                )
                val items = resultList[userItem.itemId] ?: mutableListOf()
                items.add(userItem)
                resultList[userItem.itemId] = items
            }
        }
        return resultList
    }

    override fun getTotalCountItemInMarket(excludeOwnerId: Int, filter: Filter): Int {
        return database.createQueryBuilder(enableSqlLog).addStatement(
            """
            SELECT COUNT(*)::INT AS total_count
            FROM (SELECT 1
                  FROM user_marketplace
                  WHERE status = 0 AND quantity > 0 AND uid_creator != $excludeOwnerId
                   ${filter.whereStatement()}
                  GROUP BY item_id, unit_price, reward_type) AS t;
        """.trimIndent(), emptyArray()
        ).executeQuery().getSFSObject(0).getInt("total_count")
    }

    override fun getMarketplaceItem(id: Int, configProductManager: IConfigItemManager): MarketplaceItem {
        val statement = """
            SELECT id,
                   type,
                   item_id,
                   list_id::text,
                   price,
                   quantity,
                   reward_type,
                   uid_creator,
                   (EXTRACT(EPOCH FROM modify_date) * 1000) ::BIGINT AS modify_date
            FROM user_marketplace
            WHERE id = ?;
        """.trimIndent()
        val result = mutableListOf<MarketplaceItem>()
        executeQuery(statement, arrayOf(id)) {
            result.add(MarketplaceItem.fromResultSet(it, configProductManager))
        }
        if (result.isEmpty()) throw CustomException("Market item with id $id not found")
        return result[0]
    }

    override fun getActivity(uid: Int): Map<Int, Activity> {
        val statement = """
            SELECT instant_id, action, item_name, source, type, item_id, price, reward_type, UNIX_TIMESTAMP(time) as time
            FROM user_activity_marketplace
            WHERE uid = ?
            """.trimIndent()
        val results: MutableMap<Int, Activity> = HashMap()
        executeQuery(statement, arrayOf(uid)) {
            val id = it.getInt("instant_id")
            results[id] = Activity.fromResultSet(it)
        }
        return results
    }

    override fun buyAutoMinePackage(
        uid: Int,
        autoMinePackage: AutoMinePackage,
        dataType: DataType,
        firstRewardType: String,
        secondRewardType: String
    ) {
        val jsonArray = JsonArray()
        listOf(autoMinePackage).forEach { jsonArray.add(it.toJsonObject()) }
        var statement = """CALL sp_user_buy_auto_mine(?,?,?,?,?::json);"""
        var params: Array<Any?> =
            arrayOf(uid, firstRewardType, secondRewardType, dataType.name, jsonArray.toString())
        if (firstRewardType == secondRewardType) {
            statement = """CALL sp_user_buy_auto_mine(?,?,?,?::json);"""
            params = arrayOf(uid, firstRewardType, dataType.name, autoMinePackage.toJsonObject().toString())
        }
        executeUpdateThrowException(statement, params)
    }

    override fun buyRockPackage(
        uid: Int,
        packageName: String,
        network: String,
        dataType: String,
        secondDataType: String
    ) {
        val statement = """CALL sp_user_buy_rock_pack(?,?,?,?,?);"""
        val params: Array<Any?> =
            arrayOf(uid, packageName, network, dataType, secondDataType)
        executeUpdateThrowException(statement, params)
    }


    override fun loadUserAutoMinePackage(uid: Int, dataType: DataType): IUserAutoMine {
        val statement = """
            SELECT (EXTRACT(EPOCH FROM start_time) * 1000)::BIGINT AS start_time,
                   (EXTRACT(EPOCH FROM end_time) * 1000)::BIGINT   AS end_time
            FROM user_auto_mine
            WHERE uid = ?
              AND type = ?;
        """.trimIndent()
        val results = mutableListOf<UserAutoMine>()
        executeQuery(statement, arrayOf(uid, dataType.name)) {
            results.add(
                UserAutoMine(
                    false,
                    it.getLong("start_time"),
                    it.getLong("end_time")
                )
            )
        }
        if (results.isNotEmpty()) {
            return results[0]
        }
        return UserAutoMine(true, 0, 0)
    }

    override fun loadAutoMinePackagePrice(uid: Int, listArrayPackage: JsonArray): ISFSArray {
        val statement = """
            SELECT *
            FROM fn_calculate_package_auto_price(?, ?::json);
        """.trimIndent()
        return executeQuery(statement, arrayOf(uid, listArrayPackage.toString()))
    }

    // FIXME: nhanc18, ko biết khôi phục fn_calculate_package_auto_price thế nào, nên bỏ luôn dataType (critical bug)
    /*override fun loadAutoMinePackagePrice(uid: Int, dataType: DataType): ISFSArray {
        val statement = """
            SELECT *
            FROM fn_calculate_package_auto_price(?, ?, ?::json);
        """.trimIndent()
        val jsonArray = JsonArray()
        AutoMinePackage.values().onEach {
            jsonArray.add(it.toJsonObject(dataType))
        }
        return executeQuery(statement, arrayOf(uid, dataType.name, jsonArray.toString()))
    }*/

    override fun resetShieldHero(
        dataType: DataType,
        uid: Int,
        hero: Hero,
        oldFinalDame: Int,
        price: Float,
        rewardType: BLOCK_REWARD_TYPE
    ) {
        val statement1 = """
            CALL sp_repair_hero_shield(?,
                                       ?,
                                       ?,
                                       ?,
                                       ?,
                                       ?,
                                       ?,
                                       ?);
        """.trimIndent()
        val params1 = arrayOf<Any?>(
            dataType.name,
            uid,
            hero.heroId,
            price,
            rewardType.name,
            rewardType.swapDepositedOrReward().name,
            oldFinalDame,
            hero.shield.toString()
        )
        executeUpdateThrowException(statement1, params1)
    }

    override fun resetShieldHeroWithRock(
        uid: Int,
        network: String,
        hero: Hero,
        oldFinalDame: Int,
        price: Float,
        rewardType: BLOCK_REWARD_TYPE
    ) {
        val statement = """
            CALL sp_repair_hero_shield_with_rock(?,
                                                 ?,
                                                 ?,                                              
                                                 ?,
                                                 ?,
                                                 ?,
                                                 ?);
        """.trimIndent()
        val params = arrayOf<Any?>(
            uid,
            network,
            hero.heroId,
            price,
            rewardType.name,
            oldFinalDame,
            hero.shield.toString()
        )
        executeUpdateThrowException(statement, params)
    }

    override fun getUserEmailInfo(uid: Int): ISFSObject {
        val statement = """
            SELECT email,
                   CASE WHEN email_verified_at IS NOT NULL THEN 1 ELSE 0 END AS verified
            FROM "user"
            WHERE id_user = ?
        """.trimIndent()
        val params = arrayOf<Any?>(uid)
        return executor.getUserEmailInfo(statement, params)
    }

    override fun registerEmail(uid: Int, email: String, verifyCode: String) {
        val statement = """
            UPDATE "user"
            SET email       = ?,
                verify_code = ?
            WHERE id_user = ?;
        """.trimIndent()
        val params = arrayOf<Any?>(email, verifyCode, uid)
        try {
            executor.registerEmail(statement, params)
        } catch (ex: SQLException) {
            if (ex.sqlState.equals("23505")) {
                throw CustomException("Email was existed", ErrorCode.SERVER_ERROR)
            } else {
                throw ex
            }
        } catch (ex: Exception) {
            throw ex
        }
    }

    override fun verifyEmail(uid: Int, verifyCode: String): Boolean {
        val statement = """
            UPDATE "user"
            SET email_verified_at = NOW() AT TIME ZONE 'utc',
                verify_code       = NULL
            WHERE id_user = ?
              AND verify_code = ?
            RETURNING CASE WHEN email_verified_at IS NOT NULL THEN 1 ELSE 0 END AS verified;
        """.trimIndent()
        return executor.verifyEmail(statement, arrayOf(uid, verifyCode))
    }

    override fun updateTrialUser(userName: String, uid: Int) {
        val statement1 = """
            UPDATE "user"
            SET "mode"  = ?
            WHERE id_user = ?;
        """.trimIndent()
        val params1 = arrayOf<Any?>(UserMode.NON_TRIAL.name, uid)
        val statement2 = """
            UPDATE user_bomber SET "hasDelete" = 1
            WHERE uid = ? AND "type" = ?
        """.trimIndent()
        val params2 = arrayOf<Any?>(uid, HeroType.TRIAL.name)
        val statement3 = """INSERT INTO log_user_trial(user_name, "type") 
                VALUES (?, ?)
            """.trimIndent()
        val params3 = arrayOf<Any?>(userName, "complete")
        database.createQueryBuilder(enableSqlLog)
            .addStatementUpdate(statement1, params1)
            .addStatementUpdate(statement2, params2)
            .addStatementUpdate(statement3, params3)
            .executeMultiQuery()
    }

    override fun syncDeposit(uid: Int, dataType: DataType, userDeposited: UserDeposited) {
        val statement = """
            CALL sp_sync_user_deposit(?, ?, ?, ?);
        """.trimIndent()
        val params = arrayOf<Any?>(uid, dataType.name, userDeposited.bcoinDeposited, userDeposited.senDeposited)
        executor.syncDeposit(statement, params)
    }

    override fun updateMiningMode(uid: Int, tokenType: TokenType) {
        val statement = """
            UPDATE "user"
            SET mining_token = ?
            WHERE id_user = ?;
        """.trimIndent()
        executeUpdateThrowException(statement, arrayOf(uid, tokenType.name))
    }

    override fun updateLogoutInfo(uid: Int, deviceType: DeviceType) {
        val statement = """
            UPDATE "user"
            SET first_logout      = CASE
                                        WHEN "user".first_logout IS NULL
                                            THEN NOW() AT TIME ZONE 'utc'
                                        ELSE "user".first_logout END,
                lastlogout        = CASE WHEN ? = ? THEN NOW() AT TIME ZONE 'utc' ELSE "user".lastlogout END,
                lastlogout_mobile = CASE WHEN ? = ? THEN NOW() AT TIME ZONE 'utc' ELSE "user".lastlogout_mobile END
            WHERE id_user = ?;
        """.trimIndent()
        val params =
            arrayOf<Any?>(deviceType.name, DeviceType.WEB.name, deviceType.name, DeviceType.MOBILE.name, uid)
        executeUpdateThrowException(statement, params)
    }

    override fun loadUserConfig(uid: Int, gachaChestSlotManager: IGachaChestSlotManager): UserConfig {
        var result: UserConfig? = null
        val statement = """
            SELECT *,
                   EXTRACT(EPOCH FROM last_claim_subscription) AS last_claim_subscription_second
            FROM user_config
            WHERE uid = ?;
        """.trimIndent()
        database.createQueryBuilder(enableSqlLog)
            .addStatement(statement, arrayOf(uid))
            .executeQuery { resultSet ->
                var freeRewardConfig: ISFSObject? = null
                if (resultSet.getObject("free_reward_config") != null) {
                    freeRewardConfig = SFSObject.newFromJsonData(resultSet.getString("free_reward_config"))
                }
                val userFreeRewardConfig = if (freeRewardConfig == null)
                    UserFreeRewardConfig(0, 0)
                else {
                    var lastGetGemsTime = 0L
                    if (freeRewardConfig.containsKey("lastTimeGetFreeGems"))
                        lastGetGemsTime = freeRewardConfig.getLong("lastTimeGetFreeGems")
                    var lastGetGoldsTime = 0L
                    if (freeRewardConfig.containsKey("lastTimeGetFreeGolds"))
                        lastGetGoldsTime = freeRewardConfig.getLong("lastTimeGetFreeGolds")
                    UserFreeRewardConfig(lastGetGemsTime, lastGetGoldsTime)
                }
                result = UserConfig(
                    resultSet.getInt("uid"),
                    gachaChestSlotManager,
                    userFreeRewardConfig,
                    resultSet.getString("gacha_chest_slots"),
                    resultSet.getString("misc_configs_json"),
                    if (resultSet.getObject("last_claim_subscription_second") != null) {
                        Instant.ofEpochSecond(resultSet.getLong("last_claim_subscription_second"))
                    } else null,
                    resultSet.getBoolean("is_no_ads"),
                    resultSet.getBoolean("is_received_first_chest_skip_time"),
                    resultSet.getBoolean("is_received_tutorial_reward"),
                    resultSet.getInt("total_costume_preset_slot"),
                )
            }
        if (result == null) {
            result = UserConfig(
                uid,
                gachaChestSlotManager,
                UserFreeRewardConfig(0, 0),
            )
        }
        return result!!
    }

    override fun updateUserGachaChestSlot(
        uid: Int,
        dataType: DataType,
        price: Int,
        slot: Int,
        newSlotJson: String
    ) {
        val statement = """CALL sp_user_buy_gacha_chest_slot(?,?,?,?,?);""".trimIndent()
        val params = arrayOf<Any?>(
            uid,
            dataType.name,
            price,
            slot,
            newSlotJson
        )
        executeUpdateThrowException(statement, params)
    }

    override fun increasePVPMatchCount(userId: Int, isWin: Boolean, gachaChestSlotManager: IGachaChestSlotManager) {
        val config = this.loadUserConfig(userId, gachaChestSlotManager)
        config.miscConfigs.inCreatePvpMatchCount(isWin)
        updateUserConfig(userId, config)
    }

    override fun loadUserMaterial(uid: Int, configProductManager: IConfigItemManager): Map<Int, UserMaterial> {
        val result = mutableMapOf<Int, UserMaterial>()
        val statement = """
            SELECT *
            FROM user_material
            WHERE uid = ?
        """.trimIndent()
        database.createQueryBuilder(enableSqlLog).addStatement(statement, arrayOf(uid)).executeQuery {
            val userMaterial = UserMaterial.fromResultSet(it, configProductManager)
            result[userMaterial.item.id] = userMaterial
        }
        return result
    }

    override fun mergerUserCrystal(
        uid: Int,
        sourceItemId: Int,
        targetItemId: Int,
        quantity: Int,
        mergerRate: Int,
        goldFee: Int,
        gemFee: Int
    ) {
        val qb = database.createQueryBuilder(enableSqlLog)
        val statement = """
            WITH _update_source AS (
                UPDATE user_material
                    SET quantity = quantity - ?
                    WHERE uid = ? AND item_id = ?)
            INSERT
            INTO user_material(uid, item_id, quantity, modify_date)
            VALUES (?, ?, DIV(?, ?), NOW() AT TIME ZONE 'utc')
            ON CONFLICT(uid,item_id) DO UPDATE
                SET quantity    = user_material.quantity + excluded.quantity,
                    modify_date = excluded.modify_date;
        """.trimIndent()
        val params = arrayOf<Any?>(quantity, uid, sourceItemId, uid, targetItemId, quantity, mergerRate)
        qb.addStatementUpdate(statement, params)
        // trừ gold
        val pair1 = QueryHelper.querySubUserBlockReward(
            uid,
            BLOCK_REWARD_TYPE.GOLD,
            goldFee.toFloat(),
            DataType.TR,
            ChangeRewardReason.MERGE_CRYSTAL
        )
        qb.addStatement(pair1.first, pair1.second)
        //trừ gem
        val pair2 = QueryHelper.querySubUserGem(uid, gemFee.toFloat(), ChangeRewardReason.MERGE_CRYSTAL)
        qb.addStatement(pair2.first, pair2.second)

        qb.executeMultiQuery()
    }

    override fun upgradeHeroTr(
        uid: Int,
        bomberId: Int,
        hp: Int,
        dmg: Int,
        speed: Int,
        range: Int,
        bomb: Int,
        upgradeConfig: UpgradeHeroTr
    ) {
        val qb = database.createQueryBuilder(enableSqlLog)
        // upgrade
        val statement = """
            INSERT INTO user_bomber_upgraded(uid, bomber_id, hp, dmg, speed, range, bomb)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uid,bomber_id)
                DO UPDATE SET hp    =user_bomber_upgraded.hp + excluded.hp,
                              dmg   =user_bomber_upgraded.dmg + excluded.dmg,
                              speed =user_bomber_upgraded.speed + excluded.speed,
                              range =user_bomber_upgraded.range + excluded.range,
                              bomb  =user_bomber_upgraded.bomb + excluded.bomb;
        """.trimIndent()
        val params = arrayOf<Any?>(uid, bomberId, hp, dmg, speed, range, bomb)
        qb.addStatementUpdate(statement, params)
        //sub material
        upgradeConfig.items.forEach {
            val pair = QueryHelper.querySubUserMaterial(uid, it.quantity, it.itemId)
            qb.addStatementUpdate(pair.first, pair.second)
        }
        // trừ gold
        val pair1 = QueryHelper.querySubUserBlockReward(
            uid,
            BLOCK_REWARD_TYPE.GOLD,
            upgradeConfig.goldFee.toFloat(),
            DataType.TR,
            ChangeRewardReason.UPGRADE_HERO_TR
        )
        qb.addStatement(pair1.first, pair1.second)
        //trừ gem
        val pair2 = QueryHelper.querySubUserGem(uid, upgradeConfig.gemFee.toFloat(), "Upgrade hero TR")
        qb.addStatement(pair2.first, pair2.second)

        qb.executeMultiQuery()
    }

    override fun saveUserMiscConfig(userId: Int, config: MiscConfigs) {
        val statement = """
            insert into user_config (uid, misc_configs_json) values (?, ?)
            on conflict (uid) do
            update set misc_configs_json = excluded.misc_configs_json
        """.trimIndent()
        executeUpdateThrowException(statement, arrayOf(userId, config.serialize()))
    }

    override fun saveUserConfigNoAds(userId: Int, isNoAds: Boolean) {
        val statement = """
            INSERT INTO user_config (uid, is_no_ads)
            VALUES (?, ?)
            ON CONFLICT (uid) DO UPDATE SET is_no_ads = excluded.is_no_ads;
        """.trimIndent()
        database.createQueryBuilder(enableSqlLog)
            .addStatement(statement, arrayOf(userId, isNoAds))
            .executeUpdate()
    }

    override fun updateUserConfig(uid: Int, userConfig: UserConfig) {
        val statement = """
            INSERT INTO user_config (uid,
                                     gacha_chest_slots,
                                     free_reward_config,
                                     misc_configs_json,
                                     last_claim_subscription,
                                     is_no_ads,
                                     is_received_first_chest_skip_time,
                                     total_costume_preset_slot,
                                     is_received_tutorial_reward)
            VALUES (?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?)
            ON CONFLICT (uid) DO UPDATE SET gacha_chest_slots=excluded.gacha_chest_slots,
                                            free_reward_config = excluded.free_reward_config,
                                            misc_configs_json = excluded.misc_configs_json,
                                            last_claim_subscription = excluded.last_claim_subscription,
                                            is_received_first_chest_skip_time = excluded.is_received_first_chest_skip_time,
                                            is_received_tutorial_reward = excluded.is_received_tutorial_reward,
                                            total_costume_preset_slot = excluded.total_costume_preset_slot,
                                            is_no_ads = excluded.is_no_ads;
        """.trimIndent()
        val params = arrayOf<Any?>(
            uid,
            Json.encodeToString(userConfig.userGachaChestSlots),
            Json.encodeToString(userConfig.userFreeRewardConfig),
            Json.encodeToString(userConfig.miscConfigs),
            userConfig.lastTImeClaimSubscription,
            userConfig.noAds,
            userConfig.isReceivedFirstChestSkipTime,
            userConfig.totalCostumePresetSlot,
            userConfig.isReceivedTutorialReward,
        )
        database.createQueryBuilder(enableSqlLog)
            .addStatement(statement, params)
            .executeUpdate()
    }

    override fun getUserSubscription(uid: Int): List<UserSubscription> {
        var results: MutableList<UserSubscription> = mutableListOf()
        val statement = """
            SELECT uid,
                   product_id,
                   EXTRACT(EPOCH FROM start_time)  AS start_time,
                   EXTRACT(EPOCH FROM end_time)    AS end_time,
                   EXTRACT(EPOCH FROM last_modify) AS last_modify,
                   token,
                   state
            FROM user_subscription
            WHERE uid = ?;
        """.trimIndent()
        database.createQueryBuilder(enableSqlLog)
            .addStatement(statement, arrayOf(uid))
            .executeQuery {
                results.add(UserSubscription.fromResultSet(it))
            }
        return results
    }

    override fun saveUserSubscription(
        uid: Int,
        product: SubscriptionProduct,
        startTime: Long,
        endTime: Long,
        packageToken: String,
        packageState: SubscriptionState,
    ) {
        val pair =
            QueryHelper.queryUpsertUserSubscription(uid, product, startTime, endTime, packageToken, packageState)
        database.createQueryBuilder(enableSqlLog)
            .addStatement(pair.first, pair.second)
            .executeUpdate()
    }

    override fun updateTournamentResult(
        result: IPvpResultInfo
    ) {
        try {
            val mode = result.mode.value
            val user1 = result.info[0].username
            val user2 = result.info[1].username
            val score1 = result.scores[0]
            val score2 = result.scores[1]

            @Language("SQL")
            val statement = """CALL sp_pvp_tournament_finish(?,?,?,?,?);""".trimIndent()
            database.createQueryBuilder(enableSqlLog)
                .addStatement(
                    statement,
                    arrayOf(
                        user1,
                        user2,
                        mode,
                        score1,
                        score2,
                    )
                ).executeUpdate()
        } catch (e: Exception) {
            _logger.error("Error when insert into pvp_tournament $e")
        }
    }

    override fun countPvpPlayedMatch(uid: Int): Int {
        val statementCountMatch = """
            SELECT CASE
                       WHEN misc_configs_json IS NULL THEN 0
                       WHEN (misc_configs_json::json ->> 'pvp_match_count') IS NULL THEN 0
                       ELSE (misc_configs_json::json ->> 'pvp_match_count')::INT END AS pvp_match_count
            FROM user_config
            WHERE uid = ?;
        """.trimIndent()
        var matchCount = 0
        database.createQueryBuilder(enableSqlLog)
            .addStatement(statementCountMatch, arrayOf(uid))
            .executeQuery {
                matchCount = it.getInt("pvp_match_count")
            }
        return matchCount
    }

    override fun deleteUserAccount(uid: Int) {
        val statement = """
             WITH _inserted AS (
                INSERT INTO user_deleted (id_user,
                                          user_name,
                                          datecreate,
                                          lastlogin,
                                          lastlogout,
                                          is_ban,
                                          hash,
                                          is_review,
                                          mining_token,
                                          default_mining_token,
                                          second_username,
                                          email,
                                          verify_code,
                                          email_verified_at,
                                          name,
                                          mode,
                                          type,
                                          lastlogout_mobile,
                                          lastlogin_mobile,
                                          first_logout)
                    SELECT id_user,
                           user_name,
                           datecreate,
                           lastlogin,
                           lastlogout,
                           is_ban,
                           hash,
                           is_review,
                           mining_token,
                           default_mining_token,
                           second_username,
                           email,
                           verify_code,
                           email_verified_at,
                           name,
                           mode,
                           type,
                           lastlogout_mobile,
                           lastlogin_mobile,
                           first_logout
                    FROM "user"
                    WHERE id_user = ?
                    RETURNING id_user)
            DELETE
            FROM "user"
            WHERE id_user = (SELECT id_user FROM _inserted);
        """.trimIndent()
        database.createQueryBuilder(true)
            .addStatementUpdate(statement, arrayOf(uid))
            .executeUpdate()
    }

    override fun activeHeroTr(uid: Int, heroId: Int) {
        val statement = """
            WITH _in_active AS (
                UPDATE user_bomber
                    SET active = 0
                    WHERE uid = ?
                        AND type = 2)
            UPDATE user_bomber
            SET active = 1
            WHERE uid = ?
              AND type = 2
              AND bomber_id = ?;
        """.trimIndent()
        database.createQueryBuilder(enableSqlLog)
            .addStatement(statement, arrayOf(uid, uid, heroId))
            .executeUpdate()
    }

    override fun loadUserOldItem(uid: Int): Set<ItemId> {
        var itemIds: Set<ItemId> = emptySet()
        val statement = """
            SELECT item_ids::text
            FROM user_old_item
            WHERE uid = ?;
        """.trimIndent()
        database.createQueryBuilder(enableSqlLog)
            .addStatement(statement, arrayOf(uid))
            .executeQuery {
                itemIds = deserializeList<ItemId>(it.getString("item_ids")).toSet()
            }
        return itemIds
    }

    override fun saveUserOldItem(uid: Int, itemIds: Set<ItemId>) {
        val statement = """
            INSERT INTO user_old_item(uid, item_ids)
            VALUES (?, ?::json)
            ON CONFLICT (uid) DO UPDATE SET item_ids = excluded.item_ids;
        """.trimIndent()
        database.createQueryBuilder(enableSqlLog)
            .addStatement(statement, arrayOf(uid, Json.encodeToString(itemIds)))
            .executeUpdate()
    }

    override fun loadUserCostumerPreset(uid: Int): List<IUserCostumePreset> {
        val result = mutableListOf<IUserCostumePreset>()
        val statement = """
            SELECT *
            FROM user_costume_preset
            WHERE uid = ?;
        """.trimIndent()
        database.createQueryBuilder(enableSqlLog)
            .addStatement(statement, arrayOf(uid))
            .executeQuery {
                result.add(UserCostumePreset.fromResultSet(it))
            }

        return result
    }

    override fun saveUserCostumerPreset(uid: Int, item: IUserCostumePreset) {
        val statement = """
            INSERT INTO user_costume_preset(id, uid, name, original_name, bomber_id, skin_ids)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE
                SET name          = excluded.name,
                    original_name = excluded.original_name,
                    bomber_id     = excluded.bomber_id,
                    skin_ids      = excluded.skin_ids;
        """.trimIndent()
        val params = arrayOf<Any?>(
            item.id,
            item.uid,
            item.name,
            item.originalName,
            item.bomberId,
            Json.encodeToString(item.skinIds)
        )
        database.createQueryBuilder(enableSqlLog)
            .addStatement(statement, params)
            .executeUpdate()
    }

    override fun queryAllUserAvatarActive(): MutableMap<Int, Int> {
        try {
            val statement = """
            SELECT uid, item_id
            FROM user_skin
            WHERE type = ? AND status = 1
        """.trimIndent()
            val result = mutableMapOf<Int, Int>()
            executeQuery(statement, arrayOf(ItemType.AVATAR.value)) {
                result[it.getInt("uid")] = it.getInt("item_id")
            }
            return result
        } catch (e: Exception) {
            _logger.error("Error when queryAllUserAvatarActive: $e")
            return mutableMapOf()
        }
    }

    override fun queryUserAvatarActive(userId: Int): Int {
        try {
            val statement = """
            SELECT item_id
            FROM user_skin
            WHERE type = ? AND status = 1 AND uid = ?
        """.trimIndent()
            val result = mutableListOf<Int>()
            executeQuery(statement, arrayOf(ItemType.AVATAR.value, userId)) {
                result.add(it.getInt("item_id"))
            }
            if (result.isNotEmpty()) {
                return result[0]
            } else {
                return -1
            }
        } catch (e: Exception) {
            _logger.error("Error when queryUserAvatarActive: $e")
            return 0
        }
    }

    override fun saveTelegramUser(idTelegram: String, deviceType: DeviceType): IUserInfo {
        try {
            // Check if user exists
            val checkUserStatement = """
            SELECT *
            FROM "user"
            WHERE user_name = ? AND type = 'TON';
        """.trimIndent()

            val checkUserResult = executeQuery(checkUserStatement, arrayOf(idTelegram))

            return if (checkUserResult.size() > 0) {
                // User exists, retrieve necessary data to fill IUserInfo
                UserInfo(checkUserResult.getSFSObject(0))
            } else {
                // User does not exist, insert new user
                val insertUserStatement = """
                INSERT INTO "user" (user_name, type)
                VALUES (?, 'TON');
            """.trimIndent()

                executeUpdateThrowException(insertUserStatement, arrayOf(idTelegram))

                // Retrieve necessary data to fill IUserInfo for the new user
                val newUserResult = executeQuery(checkUserStatement, arrayOf(idTelegram))
                UserInfo(newUserResult.getSFSObject(0))
            }
        } catch (e: Exception) {
            _logger.error("Error when save Telegram user: $e")
            throw e
        }
    }

    override fun createTonTransaction(uid: Int): Int {
        val statement = """
        INSERT INTO user_ton_transactions (uid, created_at)
        VALUES (?, NOW())
        RETURNING id;
    """.trimIndent()

        return try {
            val resultSet = mutableListOf<Int>()
            executeQuery(statement, arrayOf(uid)) {
                resultSet.add(it.getInt("id"))
            }
            resultSet[0]

        } catch (e: Exception) {
            _logger.error("Error when creating TON transaction: $e")
            throw e
        }
    }

    override fun updateTonTransaction(id: Int, amount: Double, txHash: String, token: String): String {
        val statement = """SELECT fn_update_user_ton_transaction(?, ?, ?, ?);""".trimIndent()

        try {
            val list = mutableListOf<String>()
            executeQueryAndThrowException(statement, arrayOf(id, amount, txHash, token)) {
                list.add(it.getString("fn_update_user_ton_transaction"))
            }

            return list[0]
        } catch (e: Exception) {
            _logger.error("Error when updating TON transaction: $e")
            return ""
        }
    }


    override fun updateBcoinTonTransaction(id: Int, amount: Double, txHash: String, token: String): String {
        val statement = """SELECT fn_update_user_bcoin_transaction(?, ?, ?, ?);""".trimIndent()

        try {
            val list = mutableListOf<String>()
            executeQueryAndThrowException(statement, arrayOf(id, amount, txHash, token)) {
                list.add(it.getString("fn_update_user_bcoin_transaction"))
            }

            return list[0]
        } catch (e: Exception) {
            _logger.error("Error when updating BCOIN transaction: $e")
            return ""
        }
    }

    override fun fusionHeroServer(
        uid: Int,
        heroIds: List<Int>,
        priceFusion: Double,
        rewardType: BLOCK_REWARD_TYPE,
        network: DataType

    ): String {
        val heroIdsString = heroIds.joinToString(",")
        val statement = """SELECT fn_fusion_hero_server(?, ?, ?, ?, ?);""".trimIndent()
        val params = arrayOf<Any?>(uid, heroIdsString, priceFusion, rewardType.name, network.name)

        try {
            val resultSet = executeQueryAndThrowException(statement, params)
            val reasonFail = resultSet.getSFSObject(0).getUtfString("fn_fusion_hero_server")
            return reasonFail ?: ""
        } catch (e: Exception) {
            _logger.error("Error when calling fn_fusion_hero_server: $e")
            return "Calling fn_fusion_hero_server fail"
        }
    }

    override fun getHeroPvp(userId: Int, heroId: Long): ISFSObject {
        val statement1 = """
        SELECT item_id
        FROM public.user_item_status
        WHERE uid = ? AND id = ?
    """.trimIndent()
        val params1 = arrayOf<Any?>(userId, heroId.toInt())

        val statement2 = """
        SELECT hp, dmg, speed, range, bomb
        FROM public.user_bomber_upgraded
        WHERE uid = ? AND bomber_id = ?
    """.trimIndent()
        val params2 = arrayOf<Any?>(userId, heroId.toInt())

        return try {
            val result = SFSObject().apply {
                putInt("item_id", 0)
                putInt("hp_upgrade", 0)
                putInt("dmg_upgrade", 0)
                putInt("speed_upgrade", 0)
                putInt("range_upgrade", 0)
                putInt("bomb_upgrade", 0)
            }

            // Execute first query
            executeQuery(statement1, params1) {
                result.putInt("item_id", it.getInt("item_id"))
            }

            // Execute second query
            executeQuery(statement2, params2) {
                result.putInt("hp_upgrade", it.getInt("hp"))
                result.putInt("dmg_upgrade", it.getInt("dmg"))
                result.putInt("speed_upgrade", it.getInt("speed"))
                result.putInt("range_upgrade", it.getInt("range"))
                result.putInt("bomb_upgrade", it.getInt("bomb"))
            }

            result
        } catch (e: Exception) {
            _logger.error("Error when getting hero PVP data: $e")
            SFSObject()
        }
    }

    override fun createSolTransaction(uid: Int): Int {
        val statement = """
        INSERT INTO user_sol_transactions (uid, created_at)
        VALUES (?, NOW())
        RETURNING id;
    """.trimIndent()

        return try {
            val resultSet = mutableListOf<Int>()
            executeQuery(statement, arrayOf(uid)) {
                resultSet.add(it.getInt("id"))
            }
            resultSet[0]

        } catch (e: Exception) {
            _logger.error("Error when creating SOL transaction: $e")
            throw e
        }
    }

    override fun updateSolTransaction(id: Int, amount: Double, txHash: String, token: String): String {
        val statement = """SELECT fn_update_user_sol_transaction(?, ?, ?, ?);""".trimIndent()

        try {
            val list = mutableListOf<String>()
            executeQueryAndThrowException(statement, arrayOf(id, amount, txHash, token)) {
                list.add(it.getString("fn_update_user_sol_transaction"))
            }

            return list[0]
        } catch (e: Exception) {
            _logger.error("Error when updating SOL transaction: $e")
            return ""
        }
    }

    override fun updateBcoinSolTransaction(id: Int, amount: Double, txHash: String, token: String): String {
        val statement = """SELECT fn_update_user_bcoin_sol_transaction(?, ?, ?, ?);""".trimIndent()

        try {
            val list = mutableListOf<String>()
            executeQueryAndThrowException(statement, arrayOf(id, amount, txHash, token)) {
                list.add(it.getString("fn_update_user_bcoin_sol_transaction"))
            }

            return list[0]
        } catch (e: Exception) {
            _logger.error("Error when updating BCOIN SOL transaction: $e")
            return ""
        }
    }

    override fun createRonTransaction(uid: Int): Int {
        val statement = """
        INSERT INTO user_ron_transactions (uid, created_at)
        VALUES (?, NOW())
        RETURNING id;
    """.trimIndent()

        return try {
            val resultSet = mutableListOf<Int>()
            executeQuery(statement, arrayOf(uid)) {
                resultSet.add(it.getInt("id"))
            }
            resultSet[0]

        } catch (e: Exception) {
            _logger.error("Error when creating RON transaction: $e")
            throw e
        }
    }

    override fun updateRonTransaction(id: Int, amount: Double, txHash: String, token: String, sender: String): String {
        val statement = """SELECT fn_update_user_ron_transaction(?, ?, ?, ?, ?);""".trimIndent()

        try {
            val list = mutableListOf<String>()
            executeQueryAndThrowException(statement, arrayOf(id, amount, txHash, token, sender)) {
                list.add(it.getString("fn_update_user_ron_transaction"))
            }

            return list[0]
        } catch (e: Exception) {
            _logger.error("Error when updating RON transaction: $e")
            return ""
        }
    }

    override fun createVicTransaction(uid: Int): Int {
        val statement = """
        INSERT INTO user_vic_transactions (uid, created_at)
        VALUES (?, NOW())
        RETURNING id;
        """.trimIndent()
        return try {
            val resultSet = mutableListOf<Int>()
            executeQuery(statement, arrayOf(uid)) {
                resultSet.add(it.getInt("id"))
            }
            resultSet[0]
        } catch (e: Exception) {
            _logger.error("Error when creating VIC transaction: $e")
            0
        }
    }

    override fun updateVicTransaction(id: Int, amount: Double, txHash: String, token: String, sender: String): String {
        val statement = """SELECT fn_update_user_vic_transaction(?, ?, ?, ?, ?);""".trimIndent()
        try {
            val list = mutableListOf<String>()
            executeQueryAndThrowException(statement, arrayOf(id, amount, txHash, token, sender)) {
                list.add(it.getString("fn_update_user_vic_transaction"))
            }
            return list[0]
        } catch (e: Exception) {
            _logger.error("Error when updating VIC transaction: $e")
            return ""
        }
    }

    override fun getOnBoardingConfig(): Map<Int, Float> {
        val statement = """
        SELECT step, reward FROM config_on_boarding;
    """.trimIndent()

        return try {
            val result = mutableMapOf<Int, Float>()
            executeQuery(statement, arrayOf()) {
                result[it.getInt("step")] = it.getFloat("reward")
            }
            result

        } catch (e: Exception) {
            _logger.error("Error when get config onboarding: $e")
            throw e
        }
    }

    override fun createBasTransaction(uid: Int): Int {
        val statement = """
        INSERT INTO user_bas_transactions (uid, created_at)
        VALUES (?, NOW())
        RETURNING id;
    """.trimIndent()

        return try {
            val resultSet = mutableListOf<Int>()
            executeQuery(statement, arrayOf(uid)) {
                resultSet.add(it.getInt("id"))
            }
            resultSet[0]

        } catch (e: Exception) {
            _logger.error("Error when creating BAS transaction: $e")
            0
        }
    }

    override fun updateBasTransaction(id: Int, amount: Double, txHash: String, token: String, sender: String): String {
        val statement = """SELECT fn_update_user_bas_transaction(?, ?, ?, ?, ?);""".trimIndent()

        try {
            val list = mutableListOf<String>()
            executeQueryAndThrowException(statement, arrayOf(id, amount, txHash, token, sender)) {
                list.add(it.getString("fn_update_user_bas_transaction"))
            }

            return list[0]
        } catch (e: Exception) {
            _logger.error("Error when updating BAS transaction: $e")
            return ""
        }
    }

    override fun getUserOnBoardingProgress(uid: Int): ISFSObject {
        val statement = """
        SELECT step, claimed FROM user_on_boarding WHERE uid = ?;
    """.trimIndent()

        try {
            val result = executeQuery(statement, arrayOf(uid))
            if (result.size() > 0) {
                return result.getSFSObject(0)
            }
            return SFSObject()
        } catch (e: Exception) {
            _logger.error("Error when get config onboarding: $e")
            throw e
        }
    }

    override fun updateUserOnBoardingProgress(userProgress: UserProgress) {
        val statement = """
        CALL sp_update_user_on_boarding(?, ?, ?, ?, ?, ?, ?);
    """.trimIndent()

        try {
            val params = arrayOf<Any?>(
                userProgress.userId,
                userProgress.newStep,
                userProgress.newClaimed,
                userProgress.currentStep,
                userProgress.currentClaimed,
                userProgress.network,
                userProgress.rewardType
            )
            executeUpdateThrowException(statement, params)
        } catch (e: Exception) {
            _logger.error("Error when updating user onboarding progress: $e")
            throw e
        }
    }

    override fun getDailyTaskConfig(): List<DailyTask> {
        try {
            val statement = """
        SELECT id, completed, reward, is_random, is_default, expired, is_deleted
        FROM daily_task_config;
    """.trimIndent()

            val result = mutableListOf<DailyTask>()
            executeQuery(statement, arrayOf()) {
                result.add(
                    DailyTask(
                        id = it.getInt("id"),
                        completed = it.getInt("completed"),
                        rewardString = it.getString("reward"),
                        isRandom = it.getBoolean("is_random"),
                        isDefault = it.getBoolean("is_default"),
                        expired = it.getLong("expired"),
                        isDeleted = it.getBoolean("is_deleted")
                    )
                )
            }
            return result
        } catch (e: Exception) {
            _logger.error("Error when get daily task config: $e")
            return emptyList()
        }
    }

    override fun getUserDailyTask(uid: Int): ISFSObject {
        val statement = """
        SELECT task_id, progress, claimed, final_reward_claimed, reward
        FROM user_daily_task
        WHERE uid = ? AND date = CURRENT_DATE;
    """.trimIndent()

        val params = arrayOf<Any?>(uid)
        val result = SFSObject()
        val tasks = SFSArray()
        try {
            val data = executeQuery(statement, params)
            if (data.size() > 0) {
                val value = data.getSFSObject(0)
                val taskIds = value.getUtfString("task_id").split(",").map { it.toInt() }
                val progresses = value.getUtfString("progress").split(",").map { it.toInt() }
                val claimedStatuses = value.getUtfString("claimed").split(",").map { it.toBoolean() }
                val rewards = value.getUtfString("reward").split(",")
                val finalRewardClaimed = value.getInt("final_reward_claimed") == 1

                for (i in taskIds.indices) {
                    val task = SFSObject()
                    task.putInt("task_id", taskIds[i])
                    task.putInt("progress", progresses[i])
                    task.putBool("claimed", claimedStatuses[i])
                    task.putUtfString("reward", rewards[i])
                    tasks.addSFSObject(task)
                }
                result.putSFSArray("tasks", tasks)
                result.putBool("final_reward_claimed", finalRewardClaimed)
            }

            return result
        } catch (e: Exception) {
            _logger.error("Error when getting current daily task: $e")
            return result

        }
    }

    override fun updateUserDailyTask(uid: Int, todayUserTask: TodayTask) {
        try {
            val finalRewardClaimed = if (todayUserTask.finalRewardClaimed) 1 else 0
            val taskIds = todayUserTask.idToString()
            val progresses = todayUserTask.progressToString()
            val claimedStatuses = todayUserTask.claimedToString()
            val rewards = todayUserTask.rewardToString()

            val statement = """
                INSERT INTO user_daily_task (uid, date, task_id, progress, claimed, final_reward_claimed, reward)
                VALUES (?, CURRENT_DATE, ?, ?, ?, ?, ?)
                ON CONFLICT (uid, date) DO UPDATE
                SET task_id = EXCLUDED.task_id,
                    progress = EXCLUDED.progress,
                    claimed = EXCLUDED.claimed,
                    final_reward_claimed = EXCLUDED.final_reward_claimed,
                    reward = EXCLUDED.reward;
                  
            """.trimIndent()

            val params = arrayOf<Any?>(uid, taskIds, progresses, claimedStatuses, finalRewardClaimed, rewards)
            executeUpdate(statement, params, false)
        } catch (e: Exception) {
            _logger.error("Error when updating user daily task: $e")
        }
    }

    override fun logTodayTask(taskId: List<Int>) {
        try {
            val taskIdsString = taskId.joinToString(",")
            val statement = """
            INSERT INTO logs.log_daily_task (today_task, date)
            VALUES (?, NOW())
        """.trimIndent()

            executeUpdate(statement, arrayOf(taskIdsString), false)
        } catch (e: Exception) {
            _logger.error("Error when logging today task: $e")
        }
    }
}
