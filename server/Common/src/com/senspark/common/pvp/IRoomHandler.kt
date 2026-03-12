package com.senspark.common.pvp

import com.smartfoxserver.v2.extensions.BaseClientRequestHandler

abstract class RoomHandler : BaseClientRequestHandler() {
    abstract val command: String
}