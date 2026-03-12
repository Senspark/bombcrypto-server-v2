package com.senspark.game.utils

import com.smartfoxserver.v2.entities.User
import javax.crypto.SecretKey

interface IUserFinder {
    fun find(username: String): Pair<User, SecretKey>?
}