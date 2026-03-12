package com.senspark.game.pvp.user

import com.smartfoxserver.v2.entities.User

interface IUserController {
    /** The currently active user. */
    val user: User?

    /** A user joins this controller. */
    fun join(user: User)

    /** The existing user leaves. */
    fun leave()
}