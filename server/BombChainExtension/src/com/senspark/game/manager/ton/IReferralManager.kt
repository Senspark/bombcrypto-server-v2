package com.senspark.game.manager.ton

import com.senspark.common.service.IServerService
import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.user.IUserInfo
import com.smartfoxserver.v2.entities.data.ISFSObject

interface IReferralManager : IServerService {
    fun setConfig(configReferralParams: MutableMap<String, Int>)
    fun getReferral(uid: Int): ISFSObject
    fun createReferral(userInfo: IUserInfo, referralCode: String)
    fun addEarning(uid: Int, addition: Double)
    fun claimRewards(userController: IUserController)
}