package com.senspark.game.api

import com.senspark.common.service.Service
import com.smartfoxserver.v2.entities.data.ISFSObject

@Service("IInternalMessageHandler")
interface IInternalMessageHandler {
    fun handle(command: String, params: ISFSObject): ISFSObject?
    fun handle(command: String, params: String): ISFSObject?
}