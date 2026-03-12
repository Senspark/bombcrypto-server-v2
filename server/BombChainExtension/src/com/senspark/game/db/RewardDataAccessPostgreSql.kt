package com.senspark.game.db

import com.senspark.common.IDatabase
import com.senspark.common.IQueryBuilder
import com.senspark.common.utils.ILogger
import com.senspark.game.constant.ItemStatus
import com.senspark.game.constant.ItemType.*
import com.senspark.game.data.model.config.AirDrop
import com.senspark.game.data.model.user.AddUserItemWrapper
import com.senspark.game.db.helper.QueryHelper
import com.senspark.game.db.model.UserAirdropClaimed
import com.senspark.game.db.model.UserStakeInfo
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.customEnum.ChangeRewardReason
import com.senspark.game.declare.customTypeAlias.ProductId
import com.senspark.game.exception.CustomException
import com.senspark.game.schema.TableBuyGemTransaction
import com.senspark.game.utils.Utils
import com.senspark.lib.db.BaseDataAccess
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class RewardDataAccessPostgreSql(
    database: IDatabase,
    log: Boolean,
    logger: ILogger,
    private val executor: RewardDataAccessExecutor = RewardDataAccessExecutor(database, log, logger)
) : BaseDataAccess(database, log, logger), IRewardDataAccess {

    override fun initialize() {
    }

    override fun addUserBlockReward(
        uid: Int,
        rewardType: BLOCK_REWARD_TYPE,
        dataType: DataType,
        value: Float,
        forControlValue: Float,
        reason: String
    ) {
        //      Làm tròn 4 chữ số tránh trường hợp bị lẻ .9999999
        val roundValue = Utils.roundAvoid(value, 4).toFloat()
        val pair = QueryHelper.queryAddUpsertUserBlockReward(
            uid,
            rewardType,
            dataType,
            roundValue + forControlValue,
            reason
        )
        executeQuery(pair.first, pair.second) {}
    }

    override fun userWithdrawStake(
        dataType: DataType,
        username: String,
        isWithDraw: Boolean
    ): UserStakeInfo {
        val statement = "SELECT * FROM fn_user_with_draw_stake(?,?,?)"
        val params = arrayOf<Any?>(username, isWithDraw, dataType.name)
        val sfsArray = executeUpdateReturnDataThrowException(statement, params)
        val sfsObject = sfsArray.getSFSObject(0)
        return UserStakeInfo(
            sfsObject.getDouble("principal"),
            sfsObject.getDouble("profit"),
            sfsObject.getLong("stake_date"),
            sfsObject.getDouble("withdraw_fee"),
            sfsObject.getDouble("total_stake"),
            sfsObject.getDouble("apd"),
            sfsObject.getDouble("receive_amount"),
        )
    }

    override fun userStake(
        username: String,
        dataType: DataType,
        blockRewardType: BLOCK_REWARD_TYPE,
        amount: Float,
        allIn: Boolean
    ): Double {
        val statement = "SELECT fn_user_stake(?,?,?::double precision,?,?) AS total_stake"
        val params = arrayOf<Any?>(username, dataType.name, amount, blockRewardType.name, allIn)
        val sfsArray = executeUpdateReturnDataThrowException(statement, params)
        val sfsObject = sfsArray.getSFSObject(0)
        return sfsObject.getDouble("total_stake")
    }

    override fun loadUserAirdropClaimed(uid: Int): HashMap<String, UserAirdropClaimed> {
        return executor.loadUserAirdropClaimed(uid)
    }

    override fun setUserClaimAirdropSuccess(uid: Int, airDropCodes: List<String>) {
        return executor.setUserClaimAirdropSuccess(uid, airDropCodes)
    }

    override fun userClaimAirdrop(
        uid: Int,
        airdrop: AirDrop,
        claimFee: Float,
        feeRewardType: BLOCK_REWARD_TYPE,
        isComplete: Int
    ) {
        return executor.userClaimAirdrop(uid, airdrop, claimFee, feeRewardType, isComplete)
    }

    override fun saveUserClaimRewardData(
        uid: Int,
        dataType: DataType,
        rewardType: BLOCK_REWARD_TYPE,
        minClaim: Float,
        apiSyncedValue: Double,
        claimConfirmed: Boolean,
    ): JsonObject {
        val statement = """
        SELECT fn_save_user_claim_reward_data(?,
                                              ?,
                                              ?,
                                              ?,
                                              ?,
                                              ?) AS value;
        """.trimIndent()
        val params = arrayOf<Any?>(uid, dataType.name, rewardType.name, minClaim, apiSyncedValue, claimConfirmed)
        val result = mutableListOf<String>()
        executeQueryAndThrowException(statement, params) { result.add(it.getString("value")) }

        return Json.parseToJsonElement(result[0]).jsonObject
    }

    override fun subUserGem(uid: Int, amount: Float): Map<String, Float> {
        val pair = QueryHelper.querySubUserGem(uid, amount, ChangeRewardReason.REVIVE_HERO)
        val list = mutableListOf<String>()
        executeQueryAndThrowException(pair.first, pair.second) {
            list.add(it.getString("sub_gem_json"))
        }
        val json = list[0]
        return Json.decodeFromString(json)

    }

    override fun subUserReward(
        uid: Int,
        rewardType: BLOCK_REWARD_TYPE,
        value: Float,
        dataType: DataType,
        reason: String
    ) {
        val pair = QueryHelper.querySubUserBlockReward(uid, rewardType, value, dataType, reason)
        executeQueryAndThrowException(pair.first, pair.second) {}
    }

    override fun checkBillTokenExist(billToken: String): Boolean {
        return transaction {
            !TableBuyGemTransaction.selectAll().where { TableBuyGemTransaction.billToken eq billToken }.empty()
        }
    }

    override fun addTRRewardForUser(
        uid: Int,
        dataType: DataType,
        rewardReceives: List<AddUserItemWrapper>,
        reloadRewardAfterAdd: () -> Unit,
        source: String,
        rewardSpent: Map<BLOCK_REWARD_TYPE, Float>,
        additionUpdateQueries: List<Pair<String, Array<Any?>>>
    ) {
        transaction {
            val queryBuilder = database.createQueryBuilder(false)
            rewardSpent.forEach {
                when (it.key) {
                    BLOCK_REWARD_TYPE.GEM -> {
                        val pair = QueryHelper.querySubUserGem(uid, it.value, source)
                        queryBuilder.addStatement(pair.first, pair.second)
                    }

                    BLOCK_REWARD_TYPE.GOLD -> {
                        val pair = QueryHelper.querySubUserBlockReward(
                            uid,
                            it.key,
                            it.value,
                            DataType.TR,
                            source
                        )
                        queryBuilder.addStatement(pair.first, pair.second)
                    }

                    BLOCK_REWARD_TYPE.COIN -> {
                        
                        val pair = QueryHelper.querySubUserBlockReward(
                            uid,
                            it.key,
                            it.value,
                            dataType.getCoinType(),
                            source
                        )
                        queryBuilder.addStatement(pair.first, pair.second)
                    }

                    else -> throw CustomException("Reward not support")
                }
            }

            rewardReceives.forEach {
                when (it.item.type) {
                    BOMB, TRAIL, WING, FIRE, EMOJI, AVATAR -> saveSkin(queryBuilder, uid, it, source)

                    HERO -> saveHero(queryBuilder, it, source)

                    BOOSTER -> saveBooster(queryBuilder, uid, it, source)

                    MATERIAL -> saveMaterial(queryBuilder, uid, it, source)

                    REWARD -> {
                        val rewardType = BLOCK_REWARD_TYPE.fromItemId(it.item.id)
                        val pair = QueryHelper.queryAddUpsertUserBlockReward(
                            uid,
                            rewardType,
                            DataType.TR,
                            it.quantity.toFloat(),
                            source
                        )
                        queryBuilder.addStatement(pair.first, pair.second)
                    }

                    else -> throw CustomException("Item type ${it.item.type.name} invalid")
                }
            }

            additionUpdateQueries.forEach {
                queryBuilder.addStatementUpdate(it.first, it.second)
            }

            queryBuilder.executeMultiQuery()
        }
        reloadRewardAfterAdd()
    }

    private fun saveSkin(queryBuilder: IQueryBuilder, uid: Int, it: AddUserItemWrapper, source: String) {
        val statement = """
                            WITH _inserted AS (
                                INSERT INTO user_skin (uid, type, status, item_id, expiration_after)
                                    SELECT ?, ?, ?, ?, ?
                                    FROM GENERATE_SERIES(1, ?)
                                    RETURNING *)
                            INSERT
                            INTO user_item_status(uid, id, item_id, status, source)
                            SELECT uid, id, item_id, ?, ?
                            FROM _inserted;
                        """.trimIndent()
        val params = arrayOf<Any?>(
            uid,
            it.item.type.value,
            if (it.isEquip) ItemStatus.LockedOrEquipSkin.value else ItemStatus.Normal.value,
            it.item.id,
            it.expirationAfter,
            it.quantity,
            if (it.isLock || it.isEquip) ItemStatus.LockedOrEquipSkin.value else ItemStatus.Normal.value,
            source
        )
        queryBuilder.addStatementUpdate(statement, params)
    }

    private fun saveMaterial(queryBuilder: IQueryBuilder, uid: Int, it: AddUserItemWrapper, source: String) {
        val statement = """
            INSERT INTO user_material(uid, item_id, quantity, modify_date)
            VALUES (?, ?, ?, NOW() AT TIME ZONE 'utc')
            ON CONFLICT (uid,item_id)
                DO UPDATE SET quantity    = user_material.quantity + excluded.quantity,
                              modify_date = NOW() AT TIME ZONE 'utc';
        """.trimIndent()
        val params = arrayOf<Any?>(
            uid,
            it.item.id,
            it.quantity,
        )
        queryBuilder.addStatementUpdate(statement, params)
    }

    private fun saveBooster(queryBuilder: IQueryBuilder, uid: Int, it: AddUserItemWrapper, source: String) {
        val statement = """
            WITH _inserted AS (
                INSERT INTO user_booster (uid, type, item_id)
                    SELECT ?, ?, ?
                    FROM GENERATE_SERIES(1, ?)
                    RETURNING *)
            INSERT
            INTO user_item_status(uid, id, item_id, status, source)
            SELECT uid, id, item_id, ?, ?
            FROM _inserted;
        """.trimIndent()
        val params = arrayOf<Any?>(
            uid,
            it.item.type.value,
            it.item.id,
            it.quantity,
            if (it.isLock) ItemStatus.LockedOrEquipSkin.value else ItemStatus.Normal.value,
            source
        )
        queryBuilder.addStatementUpdate(statement, params)
    }

    private fun saveHero(queryBuilder: IQueryBuilder, it: AddUserItemWrapper, source: String) {
        it.hero ?: throw CustomException("Hero cannot be null")
        val pair = QueryHelper.queryInsertNewHeroTR(it.hero, it.quantity, DataType.TR)
        val statement = """
                            WITH _inserted AS (
                                    ${pair.first}
                                )
                            INSERT
                            INTO user_item_status(uid, id, item_id, status, source)
                            SELECT uid, bid, ?, CASE hero_tr_type WHEN 'HERO' THEN 1 ELSE ? END, ?
                            FROM _inserted;
                        """.trimIndent()
        val params = pair.second.plus(it.item.id)
            .plus(if (it.isLock) ItemStatus.LockedOrEquipSkin.value else ItemStatus.Normal.value)
            .plus(source)
        queryBuilder.addStatementUpdate(statement, params)
    }
}