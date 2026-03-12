package com.senspark.game.utils

import com.senspark.common.utils.ILogger

class ThreadHelper(
    private val _tag: String,
    private val _logger: ILogger,
) {
    private val _locker = Any()
    private var _flag = false

    fun begin() {
        synchronized(_locker) {
            if (_flag) {
                log("race condition detected")
            }
            _flag = true
        }
    }

    fun end() {
        synchronized(_locker) {
            _flag = false
        }
    }

    private fun log(message: String) {
        _logger.log("$_tag $message")
    }
}