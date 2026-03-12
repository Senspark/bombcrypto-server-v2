package com.senspark.game.extension.events

import com.senspark.game.api.login.ILoginManager
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.extension.GlobalServices
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.extension.modules.ServerType
import com.senspark.game.manager.ton.IReferralManager

class LoginStrategyTon(services: GlobalServices) : ILoginStrategy {
    private val _serverType = ServerType.TON
    private val _netServices = services.get<ISvServicesContainer>().get(_serverType)
    private val _loginManager = _netServices.get<ILoginManager>()
    private val _referralManager = _netServices.get<IReferralManager>()

    override fun login(userName: String, loginToken: String, extra: LoginExtraData): IUserInfo {
        val u = _loginManager.loginTon(userName, loginToken, extra.deviceType)
        u.serverType = _serverType
        return u
    }

    override fun postLogin(userInfo: IUserInfo, extra: LoginExtraData) {
        if (extra.referralCode != null) {
            _referralManager.createReferral(userInfo, extra.referralCode)
        }
    }
}