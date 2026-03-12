package com.senspark.lib.db

import com.senspark.common.service.IGlobalService
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.declare.EnumConstants.UserType
import java.sql.Timestamp

interface ILibDataAccess : IGlobalService {
    fun loadUserWhiteList(): List<String>
    fun loadGameConfig(): Map<String, String>
    fun getUserInfo(idUser: Int): IUserInfo?
    fun getUserInfoByUsername(username: String): IUserInfo?
    fun getUserInfoBySecondUsername(username: String): IUserInfo?
    fun insertNewUser(username: String, accountType: UserType): Boolean
    fun updateHash(userId: Int, hash: String): Boolean
    fun updateUnBanUser(userId: Int): Boolean
    fun updateBanUser(userId: Int, banReason: String, banExpired: Timestamp?): Boolean
    fun updateIsReview(userId: Int, isReview: Int): Boolean
    fun queryBannedCountries(): Set<String>
    fun getUserHash(uid: Int): String
}