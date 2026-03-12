package com.senspark.game.manager.ton

import java.math.BigDecimal
import java.math.RoundingMode

class ClubHelper {
    companion object {
        fun calculateBidPrice(
            bidUnitPrice: Float,
            bidQuantity: Int
        ): Float {
            return bidUnitPrice.toBigDecimal().multiply(BigDecimal(bidQuantity)).setScale(1, RoundingMode.HALF_UP)
                .toFloat()
        }
    }
}