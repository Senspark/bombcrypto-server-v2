package com.senspark.game.pvp

import com.senspark.common.utils.ColorCode
import com.senspark.common.utils.ILogger

class PrefixLogger(
    private val _prefix: String,
    private val _logger: ILogger,
) : ILogger {
    override fun log2(visibleTag: String, compressibleMessage: String, customColor: ColorCode) {
        val tag = "$_prefix$visibleTag"
        _logger.log2(tag, compressibleMessage, customColor)
    }

    override fun log2(visibleTag: String, compressibleMessage: () -> String, customColor: ColorCode) {
        val tag = "$_prefix$visibleTag"
        _logger.log2(tag, compressibleMessage, customColor)
    }

    override fun log(message: String, customColor: ColorCode) {
        _logger.log("$_prefix$message", customColor)
    }

    override fun warn(message: String) {
        _logger.warn("$_prefix$message")
    }

    override fun error(message: String) {
        _logger.error("$_prefix$message")
    }

    override fun error(ex: Exception) {
        _logger.error(ex)
    }

    override fun error(prefix: String, ex: Exception) {
        _logger.error("$_prefix$prefix", ex)
    }
}