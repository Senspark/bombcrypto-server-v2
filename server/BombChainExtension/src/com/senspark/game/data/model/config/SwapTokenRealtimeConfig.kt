package com.senspark.game.data.model.config

class SwapTokenRealtimeConfig(
    var data: HashMap<String, String>
) {
    companion object {
        private const val GEM_PRICE_DOLLAR = "gem_price_dollar"
        private const val TIMES_SWAP_EACH_DAY = "times_swap_each_day"
        private const val MAX_AMOUNT_DOLLAR_EACH_TIME = "max_amount_dollar_each_time"
        private const val TOTAL_DOLLAR_SWAP_EACH_DAY = "total_dollar_swap_each_day"
        private const val TIME_MINUTE_UPDATE_PRICE = "time_minute_update_price"
        private const val REMAINING_TOTAL_DOLLAR_SWAP = "remaining_total_dollar_swap"
        private const val MIN_GEM_SWAP = "min_gem_swap"
    }

    val priceGem = data[GEM_PRICE_DOLLAR]?.toFloat() ?: 0.01f
    val timesSwapEachDay = data[TIMES_SWAP_EACH_DAY]?.toInt() ?: 1
    val maxAmountEachTime = data[MAX_AMOUNT_DOLLAR_EACH_TIME]?.toInt() ?: 10
    val totalSwapEachDay = data[TOTAL_DOLLAR_SWAP_EACH_DAY]?.toInt() ?: 1000
    val timeUpdatePrice = data[TIME_MINUTE_UPDATE_PRICE]?.toInt() ?: 60
    val remainingTotalSwap = data[REMAINING_TOTAL_DOLLAR_SWAP]?.toFloat() ?: 1000f
    val minGemSwap = data[MIN_GEM_SWAP]?.toInt() ?: 50
}