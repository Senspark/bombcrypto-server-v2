package com.senspark.common.pvp

import com.senspark.common.utils.ILogger
import com.smartfoxserver.v2.entities.User

interface IRoomExtension {
    val controller: IMatchController

    /** Initializes this pvp extension (room extension). */
    fun initialize(factory: IMatchFactory, logger: ILogger)

    fun release()

    /** Let the specified user joins this room. */
    fun join(user: User, isObserver: Boolean)

    /** Let the specified user leaves this room. */
    fun leave(user: User)

    /** Kicks the specified user. */
    fun kick(user: User, reason: String)
}