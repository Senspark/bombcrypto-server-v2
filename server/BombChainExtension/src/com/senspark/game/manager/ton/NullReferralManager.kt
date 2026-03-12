package com.senspark.game.manager.ton

import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.user.IUserInfo
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class NullReferralManager : IReferralManager {
    override fun initialize() {
    }
    
    override fun setConfig(configReferralParams: MutableMap<String, Int>) {}

    override fun getReferral(uid: Int): ISFSObject {
        return SFSObject()
    }

    override fun createReferral(userInfo: IUserInfo, referralCode: String) {}

    override fun addEarning(uid: Int, addition: Double) {}

    override fun claimRewards(userController: IUserController) {}
}