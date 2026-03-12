package com.senspark.common.pvp

import com.senspark.common.utils.ILogger

interface IMatchFactory {
    val controller: IMatchController
    val handlers: List<RoomHandler>
    fun initialize(logger: ILogger)
    fun destroy()
}
