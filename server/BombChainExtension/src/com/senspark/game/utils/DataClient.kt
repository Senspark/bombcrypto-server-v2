package com.senspark.game.utils

import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

object DataClient {
    fun convertResponse(response: ISFSObject): ISFSObject {
        return SFSObject().apply { putUtfString("data", response.toJson()) }
    }

    fun convertRequest(request: ISFSObject): ISFSObject {
        return SFSObject.newFromJsonData(request.getUtfString("data"))
    }
}