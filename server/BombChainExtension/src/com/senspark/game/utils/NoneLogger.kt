package com.senspark.game.utils

import com.senspark.common.utils.ColorCode
import com.senspark.common.utils.ILogger

class NoneLogger : ILogger {
    override fun log2(visibleTag: String, compressibleMessage: String, customColor: ColorCode) {
    }

    override fun log2(visibleTag: String, compressibleMessage: () -> String, customColor: ColorCode) {
    }

    override fun log(message: String, customColor: ColorCode) {
    }

    override fun warn(message: String) {
    }

    override fun error(message: String) {
    }

    override fun error(ex: Exception) {
    }

    override fun error(prefix: String, ex: Exception) {
    }
}