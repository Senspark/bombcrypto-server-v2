package com.senspark.game.extension.events

import com.senspark.common.utils.IGlobalLogger
import com.senspark.game.declare.SFSCommand
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.handler.MainGameExtensionBaseEventHandler
import com.senspark.game.manager.IUsersManager
import com.senspark.game.manager.online.IUserOnlineManager
import com.senspark.game.manager.treasureHuntV2.ITreasureHuntV2Manager
import com.senspark.game.user.ITrGameplayManager
import com.smartfoxserver.v2.core.ISFSEvent
import com.smartfoxserver.v2.core.SFSEventParam
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.exceptions.SFSException
import java.util.*

class UserLogoutHandler : MainGameExtensionBaseEventHandler() {
    
    private val _logger = globalServices.get<IGlobalLogger>()
    private val _userOnlineManager = globalServices.get<IUserOnlineManager>()

    @Throws(SFSException::class)
    override fun handleServerEvent(iSFSEvent: ISFSEvent) {
        val user = iSFSEvent.getParameter(SFSEventParam.USER) as User?
        if (user == null) {
            _logger.error("UserLogoutHandler null user")
            return
        }
        if (user.containsProperty("processing_logout")) {
            _logger.log("UserLogoutHandler already logout")
            return
        }
        user.setProperty("processing_logout", 1)
        updateUserLogout(user)
    }

    private fun updateUserLogout(user: User) {
        try {
            val userController = globalServices.get<ISvServicesContainer>()
                .filter(IUsersManager::class)
                .firstNotNullOf { e -> e.getUserController(user) }


            userController.logger.log("User logout: ${userController.userName}")

            // remove user này ra khỏi ds đang chơi pvp và adventure mode
            val trGamePlayManager = globalServices.get<ITrGameplayManager>()
            trGamePlayManager.leaveAll(userController.userId, userController.dataType)

            val treasureHuntV2Manager = userController.svServices.get<ITreasureHuntV2Manager>()
            val usersManager = userController.svServices.get<IUsersManager>()
            
            // Remove user from Redis online tracking using UserOnlineManager
            val userId = userController.userId
            _userOnlineManager.removeUserOnline(userId)

            userController.logOut()
            user.joinedRooms.forEach { r ->
                r.extension.handleInternalMessage(SFSCommand.USER_LOGOUT, user)
            }

            usersManager.remove(userController)
            treasureHuntV2Manager.leaveRoom(userController)
        } catch (e: Exception) {
            _logger.error("UserLogoutHandler error: ${e.message}")
        }
    }
}