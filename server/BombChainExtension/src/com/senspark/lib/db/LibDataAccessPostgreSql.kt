package com.senspark.lib.db

import com.senspark.common.IDatabase
import com.senspark.common.utils.ILogger
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.declare.EnumConstants
import java.sql.Timestamp

class LibDataAccessPostgreSql(
    database: IDatabase,
    log: Boolean,
    logger: ILogger,
    private val executor: LibDataAccessExecutor = LibDataAccessExecutor(database, log, logger)
) : BaseDataAccess(database, log, logger), ILibDataAccess {

    override fun initialize() {
    }

    override fun loadGameConfig(): HashMap<String, String> {
        val statement = """SELECT * FROM game_config;"""
        return executor.loadGameConfig(statement)
    }

    override fun loadUserWhiteList(): List<String> {
        val statement = """SELECT * FROM user_white_list WHERE active = 1"""
        return executor.loadUserWhiteList(statement)
    }

    override fun getUserInfo(idUser: Int): IUserInfo {
        val stmt = """
            SELECT *, EXTRACT(EPOCH FROM datecreate)::BIGINT AS datecreate_mili
            FROM "user"
            WHERE id_user = ?"""
        return executor.getUserInfo(stmt, idUser)
    }

    override fun getUserInfoByUsername(username: String): IUserInfo? {
        val statement =
            """SELECT *, EXTRACT(EPOCH FROM datecreate)::BIGINT AS datecreate_mili FROM "user" WHERE user_name = ?;"""
        return executor.getUserInfoByWalletAddress(statement, username)
    }

    override fun getUserInfoBySecondUsername(username: String): IUserInfo? {
        val statement =
            """SELECT *, EXTRACT(EPOCH FROM datecreate)::BIGINT AS datecreate_mili FROM "user" WHERE "second_username"= ?;"""
        return executor.getUserInfoBySecondUsername(statement, username)
    }

    override fun insertNewUser(username: String, accountType: EnumConstants.UserType): Boolean {
        val statement = """INSERT INTO "user" (user_name,type) VALUES (?,?);"""
        return executeUpdate(statement, arrayOf(username, accountType.name))
    }

    override fun updateHash(userId: Int, hash: String): Boolean {
        val statement = """UPDATE "user" SET hash = ? WHERE id_user = ?;""".trimIndent()
        return try {
            executeUpdate(statement, arrayOf(hash, userId))
        } catch (e: Exception) {
            false
        }
    }


    override fun updateUnBanUser(userId: Int): Boolean {
        val stmt = """UPDATE "user" SET "is_ban" = ? WHERE "id_user" = ?"""
        return executor.updateIsBan(stmt, userId, 0)
    }

    override fun updateBanUser(userId: Int, banReason: String, banExpired: Timestamp?): Boolean {
        val stmt = """
            UPDATE "user" 
            SET "is_ban" = ?,
                "ban_reason" = ?,
                "ban_at" = CURRENT_TIMESTAMP,
                "ban_expired_at" = ?
            WHERE "id_user" = ?
            """.trimMargin()
        return executeUpdate(stmt, arrayOf(1, banReason, banExpired, userId))
    }

    override fun updateIsReview(userId: Int, isReview: Int): Boolean {
        val stmt = """UPDATE "user" SET "is_review" = "is_review" + ? WHERE "id_user" = ?"""
        return executor.updateIsReview(stmt, userId, isReview)
    }

    override fun queryBannedCountries(): Set<String> {
        val statement = "SELECT * FROM banned_country"
        return executor.queryBannedCountries(statement)
    }

    override fun getUserHash(uid: Int): String {
        val statement = """SELECT hash FROM "user" WHERE id_user = ?;"""
        val list = mutableListOf<String>()
        executeQueryAndThrowException(statement, arrayOf(uid)) {
            list.add(it.getString("hash"))
        }
        return list[0]
    }
}