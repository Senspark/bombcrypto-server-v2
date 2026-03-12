package com.senspark.game.exception

class HackException(
    val hackType: Int = 11,
    val ban: Boolean = false,
    message: String
) : Exception(message)