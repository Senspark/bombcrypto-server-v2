package com.senspark.game.data.manager.nativeRate

import com.senspark.common.service.IGlobalService
import com.senspark.game.declare.EnumConstants.DataType

/**
 * Native coin charged per 1 BCOIN of list price, per network — display side only, fn_native_price in
 * SQL is the authority for what is actually charged.
 */
interface INativeRateManager : IGlobalService {
    fun setConfig(rates: Map<DataType, Double>)

    /** null when the network has no native balance, or no rate configured yet. */
    fun nativePerBcoin(dataType: DataType): Double?

    fun nativePrice(dataType: DataType, bcoinPrice: Double): Double?

    fun dump(): String
}
