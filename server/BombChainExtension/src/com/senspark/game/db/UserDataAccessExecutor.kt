package com.senspark.game.db

import com.senspark.common.IDatabase
import com.senspark.common.utils.ILogger
import com.senspark.game.data.model.user.UserStakeVipReward
import com.senspark.game.declare.EnumConstants.StakeVipRewardType
import com.senspark.game.declare.EnumConstants.TokenType
import com.senspark.lib.db.BaseDataAccess
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class UserDataAccessExecutor(
    database: IDatabase,
    log: Boolean,
    logger: ILogger,
) : BaseDataAccess(database, log, logger) {

    fun changeMiningToken(uid: Int, tokenType: TokenType): Boolean {
        val statement = """UPDATE "user" SET mining_token = ? WHERE id_user = ?;"""
        val params = arrayOf<Any?>(tokenType.name, uid)
        return executeUpdate(statement, params)
    }

    fun changeDefaultMiningToken(uid: Int, tokenType: TokenType): Boolean {
        val statement = """UPDATE "user" SET default_mining_token = ? WHERE id_user = ?;"""
        val params = arrayOf<Any?>(tokenType.name, uid)
        return executeUpdate(statement, params)
    }

    fun loadUserStakeVip(statement: String, username: String): List<UserStakeVipReward> {
        val params = arrayOf<Any?>(username, username)
        val results = executeQuery(statement, params)
        val size: Int = results.size()
        val rewards: MutableList<UserStakeVipReward> = ArrayList()
        for (i in 0 until size) {
            val rs: ISFSObject = results.getSFSObject(i)
            rewards.add(
                UserStakeVipReward(
                    rs.getInt("level"),
                    StakeVipRewardType.valueOf(rs.getUtfString("reward_type")),
                    rs.getUtfString("type"),
                    rs.getInt("quantity"),
                    rs.getInt("having_quantity"),
                    if (rs.isNull("next_claim")) 0 else rs.getLong("next_claim")
                )
            )
        }
        return rewards.toList()
    }

    fun claimStakeVipReward(
        statement1: String,
        params1: Array<Any?>,
        statement2: String,
        params2: Array<Any?>,
    ) {
//        val wrappers = listOf(
//            ExecuteWithTransactionWrapper(statement1, params1),
//            ExecuteWithTransactionWrapper(statement2, params2)
//        )
//        executeUpdateMultiQueriesWithTransaction(wrappers)
    }

    //    FIXME: 2022/04/26 Do chức năng này đã có và nằm bên pvp nên làm tạm thời đề hiển thị quà cho vip stake
    fun getUserPvpBoosters(statement: String, uid: Int): SFSArray {
        val results = executeQuery(statement, arrayOf<Any?>(uid))
        val size: Int = results.size()
        val result = SFSArray()
        for (i in 0 until size) {
            val rs: ISFSObject = results.getSFSObject(i)
            val item = SFSObject()
            item.putUtfString("type", rs.getUtfString("type"))
            item.putInt("quantity", rs.getInt("quantity"))
            result.addSFSObject(item)
        }
        return result
    }

    fun activateCode(statement: String, userId: Int, code: String): Boolean {
        val result = executeQuery(statement, arrayOf<Any?>(userId, code))
        var actvated = false
        if (result.size() > 0) {
            actvated = result.getSFSObject(0).getInt("activated") > 0
        }
        return actvated
    }

    fun logRepairShield(statement: String) {
        executeUpdateThrowException(statement, arrayOf())
    }

    fun getUserEmailInfo(statement: String, params: Array<Any?>): ISFSObject {
        return executeQuery(statement, params).getSFSObject(0)
    }

    fun registerEmail(statement: String, params: Array<Any?>) {
        executeUpdateThrowException(statement, params)
    }

    fun verifyEmail(statement: String, params: Array<Any?>): Boolean {
        val result = executeQuery(statement, params)
        if (result.size() > 0) {
            return result.getSFSObject(0).getInt("verified") == 1
        }
        return false
    }

    fun removeEmailIfNotVerify(statement: String, params: Array<Any?>) {
        return executeUpdateThrowException(statement, params)
    }

    fun syncDeposit(statement: String, params: Array<Any?>) {
        return executeUpdateThrowException(statement, params)
    }
}