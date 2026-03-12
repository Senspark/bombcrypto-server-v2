package com.senspark.game.manager

import com.senspark.common.service.IServerService
import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.declare.EnumConstants
import com.senspark.game.extension.GlobalServices
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.extensions.SFSExtension

interface IUsersManager : IServerService {
    fun getUserId(userName: String): Int
    fun getUserController(userName: String): IUserController?
    fun getUserController(user: User): IUserController?
    
    // Legacy method - will return the first available controller or null
    fun getUserController(userId: Int): IUserController?
    
    // New method with DataType - should be used going forward
    fun getUserController(userId: Int, dataType: EnumConstants.DataType): IUserController?
    
    fun checkExistence(userController: IUserController): Boolean
    fun createUserController(
        extension: SFSExtension,
        services: GlobalServices,
        user: User,
        userInfo: IUserInfo,
        factory: (userInfo: IUserInfo)-> IUserController,
        onCompleted: (userController: IUserController?) -> Unit
    )

    fun remove(userName: String)
    fun remove(userController: IUserController)
    fun kickAndRemoveUser(userId: Int)
    fun kickAndRemoveUser(userName: String)
    fun isUserLoggedOut(userId: Int, dataType: EnumConstants.DataType): Boolean
    fun isLoggedIn(userId: Int, dataType: EnumConstants.DataType): Boolean
    fun updateKeepAliveTime(userId: Int, dataType: EnumConstants.DataType)
    fun safeCheckAndDisposeOldSession(userName: String)
    fun dispose()

    /**
     * Toggles client logging for a specific user ID
     * @param uid User ID to enable/disable logging for
     * @param sendLog true to enable logging, false to disable it
     */
    fun setClientLogging(uid: Int, sendLog: Boolean)

    /**
     * Checks if client logging is enabled for a specific user ID
     * @param uid User ID to check
     * @return true if logging is enabled for the user, false otherwise
     */
    fun isClientLoggingEnabled(uid: Int): Boolean
    fun getAllUserControllersOfUid(uid: Int): List<IUserController>
}