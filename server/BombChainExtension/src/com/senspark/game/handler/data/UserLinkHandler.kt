package com.senspark.game.handler.data

import com.senspark.game.api.IAuthApi
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.user.IUserLinkManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class UserLinkHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.USER_LINK_V2

    private val _authApi = services.get<IAuthApi>()

    // Sửa lại đơn giản hơn, sau khi client gọi api tạo acc mới thì trả uid về để ở đây server dùng để link luôn
    // Ko cần gọi api verifyAuth nữa sau đó client sẽ tự login lại như 1 account bth
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            
            if(controller.userInfo.type != EnumConstants.UserType.GUEST) {
                throw Exception("User is not guest")
            }

            val userLinkManager = controller.svServices.get<IUserLinkManager>()
            val response = SFSObject()
            var uid = 0;
            val userName = data.getUtfString("username")
            val password = data.getUtfString("password")
            val email = data.getUtfString("email")
            
            val isOldClient  = userName.isNullOrEmpty() || password.isNullOrEmpty() || email.isNullOrEmpty()
            
            // Support client cũ
            if(isOldClient) {
                val token = data.getUtfString("token")
                val info = _authApi.verifyAuthTr(controller.userName, token)
                uid = info.userId
            }
            // Client mới
            else{
                uid = _authApi.createAccountSenspark(userName, password, email)
            }
            
            // Tạo account thành công, giờ link với account guest
            if(uid > 0) {
                userLinkManager.link(controller.userId, uid)
                response.putBool("result", true)
            }
            else{
                response.putBool("result", false)
            }   
            sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}