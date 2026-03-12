package com.senspark.game.api

import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class NullServerInfoManager : IServerInfoManager {

    override fun initialize() {
    }

    override fun reloadUserOnline() {}

    override fun isEnable(): Boolean {
        return false
    }

    override fun getServerInfo(): ISFSObject {
        return SFSObject()
    }

    override fun getServerInfoTimeUpdate(): Int {
        return 0
    }
}