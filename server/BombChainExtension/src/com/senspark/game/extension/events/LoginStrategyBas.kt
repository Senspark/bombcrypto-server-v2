package com.senspark.game.extension.events

import com.senspark.game.api.login.ILoginManager
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.extension.GlobalServices
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.extension.modules.ServerType

class LoginStrategyBas(services: GlobalServices) : ILoginStrategy {
    private val _serverType = ServerType.BAS
    private val _loginManager = services.get<ISvServicesContainer>().get(_serverType).get<ILoginManager>()

    override suspend fun login(userName: String, loginToken: String, extra: LoginExtraData): IUserInfo {
        val u = _loginManager.loginBas(userName, loginToken, extra.deviceType)
        u.serverType = _serverType
        return u
    }

    override suspend fun postLogin(userInfo: IUserInfo, extra: LoginExtraData) {
    }
}
