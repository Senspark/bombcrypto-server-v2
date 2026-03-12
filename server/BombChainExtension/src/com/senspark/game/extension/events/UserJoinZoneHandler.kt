package com.senspark.game.extension.events

import com.senspark.game.api.login.ILoginManager
import com.senspark.game.controller.AirdropUserController
import com.senspark.game.controller.IUserController
import com.senspark.game.controller.LegacyUserController
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSField
import com.senspark.game.declare.UserNameSuffix
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.extension.modules.ServerType
import com.senspark.game.handler.MainGameExtensionBaseEventHandler
import com.senspark.game.manager.IUsersManager
import com.senspark.game.manager.ton.IForceLoginManager
import com.smartfoxserver.v2.core.ISFSEvent
import com.smartfoxserver.v2.core.SFSEventParam
import com.smartfoxserver.v2.core.SFSEventType
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import com.smartfoxserver.v2.exceptions.SFSException

class UserJoinZoneHandler : MainGameExtensionBaseEventHandler() {

    override fun handleServerEvent(event: ISFSEvent) {
        val user = event.getParameter(SFSEventParam.USER) as User?
        user ?: throw SFSException("Null user")

        val userInfo = user.session.getProperty(SFSField.UserInfo) as IUserInfo
        val newUser = user.session.getProperty(SFSField.NewUser) as Boolean

        val netServices = globalServices.get<ISvServicesContainer>().get(userInfo.serverType)
        val loginManger = netServices.get<IForceLoginManager>();

        // Chỉ cho phép user có session hash đã force login ở login handler vào
        // các user khác sẽ bị kick
        if (loginManger.checkToKickUser(userInfo.username, user.session.hashId)) {
            api.kickUser(user, null, "kick", 1)
            return
        }

        // Do account Fi và wallet của account đó có thể login trùng network nhưng ở phase login trước chưa đủ thông
        // tin để check xem có kick ko nên phải cho login vô tới phase này có đc uid rồi mới check đc
//        if(loginManger.checkToKickAccountFi(userInfo.id, userInfo.dataType)) {
//            api.kickUser(user, null, "kick", 1)
//            return
//        }
        
        val usersManager = netServices.get<IUsersManager>()
        usersManager.createUserController(parentExtension, globalServices, user, userInfo, ::createController) {
            if (it != null) {
                sendResponseToUser(it, newUser, userInfo)
                it.logger.log("${it.userName} join zone")
            }
        }
    }

    private fun sendResponseToUser(
        controller: IUserController,
        newUser: Boolean,
        userInfo: IUserInfo
    ) {
        val data: ISFSObject = SFSObject()
        data.putInt(SFSField.ErrorCode, ErrorCode.SUCCESS)
        data.putBool(SFSField.NewUser, newUser)
        data.putUtfString(SFSField.ADDRESS, UserNameSuffix.removeSuffixName(userInfo.username))

        // Kiểm tra xem user này có cần phải gửi log không
        val netServices = globalServices.get<ISvServicesContainer>().get(userInfo.serverType)
        val usersManager = netServices.get<IUsersManager>()
        if(usersManager.isClientLoggingEnabled(controller.userId)) {
            data.putBool(SFSField.SEND_LOG, true);
        }
        data.putUtfString(
            SFSField.TOKEN_TYPE,
            EnumConstants.TokenType.BCOIN.name
        ) // FIXME: remove from client in the future
        data.putBool("has_passcode", false) // FIXME: remove from client in the future

        if (userInfo.name != null) data.putUtfString("name", userInfo.name) else data.putNull("name") // FIXME: ???
        controller.sendDataEncryption(SFSEventType.USER_LOGIN.toString(), data)
    }

    private fun createController(userInfo: IUserInfo): IUserController {
        return when {
            userInfo.isAirdropUser() -> AirdropUserController(parentExtension, userInfo, globalServices)
            else -> LegacyUserController(parentExtension, userInfo, globalServices)
        }
    }
}