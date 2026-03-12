package com.senspark.common.utils

class PrintLogger : ILogger {

    companion object {
        private const val ANSI_RESET: String = "\u001B[0m"
        private const val ANSI_BLACK: String = "\u001B[30m"
        private const val ANSI_RED: String = "\u001B[31m"
        private const val ANSI_GREEN: String = "\u001B[32m"
        private const val ANSI_YELLOW: String = "\u001B[33m"
        private const val ANSI_BLUE: String = "\u001B[34m"
        private const val ANSI_PURPLE: String = "\u001B[35m"
        private const val ANSI_CYAN: String = "\u001B[36m"
        private const val ANSI_WHITE: String = "\u001B[37m"

        private const val ANSI_BLACK_BACKGROUND: String = "\u001B[40m"
        private const val ANSI_RED_BACKGROUND: String = "\u001B[41m"
        private const val ANSI_GREEN_BACKGROUND: String = "\u001B[42m"
        private const val ANSI_YELLOW_BACKGROUND: String = "\u001B[43m"
        private const val ANSI_BLUE_BACKGROUND: String = "\u001B[44m"
        private const val ANSI_PURPLE_BACKGROUND: String = "\u001B[45m"
        private const val ANSI_CYAN_BACKGROUND: String = "\u001B[46m"
        private const val ANSI_WHITE_BACKGROUND: String = "\u001B[47m"
    }

    override fun log2(visibleTag: String, compressibleMessage: String, customColor: ColorCode) {
        val colorCode = colorCodeToString(customColor)
        println("$colorCode$visibleTag $compressibleMessage $ANSI_RESET")
    }

    override fun log2(visibleTag: String, compressibleMessage: () -> String, customColor: ColorCode) {
        val colorCode = colorCodeToString(customColor)
        val msg = compressibleMessage()
        println("$colorCode$visibleTag $msg $ANSI_RESET")
    }

    override fun log(message: String, customColor: ColorCode) {
        val colorCode = colorCodeToString(customColor)
        println("$colorCode$message$ANSI_RESET")
    }

    override fun warn(message: String) {
        println("$ANSI_YELLOW$message$ANSI_RESET")
    }

    override fun error(message: String) {
        println("$ANSI_RED$message$ANSI_RESET")
    }

    override fun error(ex: Exception) {
        print(ANSI_RED)
        ex.printStackTrace()
        print(ANSI_RESET)
    }

    override fun error(prefix: String, ex: Exception) {
        print(ANSI_RED)
        println("${prefix}: ${ex.message}")
        ex.printStackTrace()
        print(ANSI_RESET)
    }
    
    private fun colorCodeToString(colorCode: ColorCode): String {
        return when (colorCode) {
            ColorCode.BLACK -> ANSI_BLACK
            ColorCode.RED -> ANSI_RED
            ColorCode.GREEN -> ANSI_GREEN
            ColorCode.YELLOW -> ANSI_YELLOW
            ColorCode.BLUE -> ANSI_BLUE
            ColorCode.PURPLE -> ANSI_PURPLE
            ColorCode.CYAN -> ANSI_CYAN
            ColorCode.WHITE -> ANSI_WHITE
        }
    }
}