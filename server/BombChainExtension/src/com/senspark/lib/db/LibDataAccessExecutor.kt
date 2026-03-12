package com.senspark.lib.db

import com.senspark.common.IDatabase
import com.senspark.common.utils.ILogger
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.data.model.user.UserInfo
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSObject

open class LibDataAccessExecutor(
    database: IDatabase,
    log: Boolean,
    logger: ILogger,
) : BaseDataAccess(database, log, logger) {

    fun loadGameConfig(statement: String): HashMap<String, String> {
        val result = HashMap<String, String>()
        executeQuery(statement, arrayOf()) {
            val key = it.getString("key")
            val value = it.getString("value")
            result[key] = value
        }
        return result
    }

    fun loadUserWhiteList(statement: String): List<String> {
        val result: MutableList<String> = ArrayList()
        executeQuery(statement, arrayOf()) {
            val userName = it.getString("user_name")
            result.add(userName)
        }
        return result
    }

    fun getUserInfo(statement: String, idUser: Int): IUserInfo {
        val sfsArray = executeQuery(statement, arrayOf(idUser))
        if (sfsArray.size() == 1) {
            return parseUserInfo(sfsArray.getSFSObject(0))
        } else throw CustomException("User $idUser not exist", ErrorCode.SERVER_ERROR)
    }

    private fun parseUserInfo(sfsObj: ISFSObject): UserInfo {
        return UserInfo(sfsObj)
    }

    fun checkUsernameUnique(statement: String, username: String): IUserInfo? {
        val sfsArray = executeQuery(statement, arrayOf(username))
        var result: UserInfo? = null
        if (sfsArray.size() == 1) {
            result = parseUserInfo(sfsArray.getSFSObject(0))
        }
        return result
    }

    fun getUserInfoByWalletAddress(statement: String, username: String): IUserInfo? {
        val sfsArray = executeQuery(statement, arrayOf(username))
        var result: UserInfo? = null
        if (sfsArray.size() >= 1) {
            result = parseUserInfo(sfsArray.getSFSObject(0))
        }
        return result
    }

    fun getUserInfoBySecondUsername(statement: String, username: String): IUserInfo? {
        val sfsArray = executeQuery(statement, arrayOf(username))
        var result: UserInfo? = null
        if (sfsArray.size() >= 1) {
            result = parseUserInfo(sfsArray.getSFSObject(0))
        }
        return result
    }

    fun updateIsBan(statement: String, userId: Int, isBan: Int): Boolean {
        return try {
            executeUpdate(statement, arrayOf(isBan, userId))
        } catch (e: Exception) {
            false
        }
    }

    fun updateIsReview(statement: String, userId: Int, isReview: Int): Boolean {
        return try {
            executeUpdate(statement, arrayOf(isReview, userId))
        } catch (e: Exception) {
            false
        }
    }

    fun updateLockClaim(statement: String, userId: Int, lockClaim: Int): Boolean {
        return try {
            executeUpdate(statement, arrayOf(lockClaim, userId))
        } catch (e: Exception) {
            false
        }
    }

    open fun queryBannedCountries(statement: String): Set<String> {
        val results: MutableSet<String> = HashSet()
        executeQuery(statement, arrayOf()) {
            results.add(it.getString("country_code"))
        }
        return results
    }
}