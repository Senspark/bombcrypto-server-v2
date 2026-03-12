package com.senspark.game.exception

import com.senspark.game.declare.ErrorCode

class CustomException(message: String, val code: Int, val willTraceLog: Boolean = false) : Exception(message) {
    constructor(message: String, code: Int) : this(message, code, false)
    constructor(message: String) : this(message, ErrorCode.SERVER_ERROR, false)
}