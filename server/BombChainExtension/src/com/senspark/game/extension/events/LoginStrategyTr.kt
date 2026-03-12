package com.senspark.game.extension.events

import com.senspark.game.api.login.ILoginManager
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.extension.GlobalServices
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.extension.modules.ServerType

class LoginStrategyTr(services: GlobalServices) : ILoginStrategy {
    private val _serverType = ServerType.BNB_POL
    private val _loginManager = services.get<ISvServicesContainer>().get(_serverType).get<ILoginManager>()

    override fun login(userName: String, loginToken: String, extra: LoginExtraData): IUserInfo {
        val u = _loginManager.loginAccount(userName, loginToken, extra.dataType, extra.deviceType)
        u.serverType = _serverType
        return u
    }

    override fun postLogin(userInfo: IUserInfo, extra: LoginExtraData) {
    }
}