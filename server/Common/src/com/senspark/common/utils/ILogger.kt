package com.senspark.common.utils

import com.senspark.common.service.IGlobalService
import com.senspark.common.service.IServerService
import com.senspark.common.service.IService

interface ILogger : IService {
    /**
     * Log có compression
     * @param visibleTag: Chuỗi này sẽ luôn xuất hiện trong log
     * @param compressibleMessage: Chuỗi này sẽ bị compress nếu quá dài
     */
    fun log2(visibleTag: String, compressibleMessage: String, customColor: ColorCode = ColorCode.GREEN)

    /**
     * Log có compression
     * @param visibleTag: Chuỗi này sẽ luôn xuất hiện trong log
     * @param compressibleMessage: Lambda (Chuỗi này sẽ bị compress nếu quá dài)
     */
    fun log2(visibleTag: String, compressibleMessage: () -> String, customColor: ColorCode = ColorCode.GREEN)
    fun log(message: String, customColor: ColorCode = ColorCode.GREEN)
    fun warn(message: String)
    fun error(message: String)
    fun error(ex: Exception)
    fun error(prefix: String, ex: Exception)
    override fun destroy() {}
}

enum class ColorCode {
    BLACK,
    RED,
    GREEN,
    YELLOW,
    BLUE,
    PURPLE,
    CYAN,
    WHITE,
}

/**
 * Logger sử dụng cho tất cả các loại Server
 */
interface IGlobalLogger : ILogger, IGlobalService

/**s
 * Logger dành riêng cho 1 từng Server: BNB/TON/SOL
 */
interface IServerLogger : ILogger, IServerService