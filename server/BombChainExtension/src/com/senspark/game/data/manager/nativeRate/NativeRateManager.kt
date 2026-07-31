package com.senspark.game.data.manager.nativeRate

import com.senspark.common.utils.ILogger
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.EnumConstants.DataType

class NativeRateManager(
    private val _shopDataAccess: IShopDataAccess,
    private val _logger: ILogger,
) : INativeRateManager {

    private val _rates: MutableMap<DataType, Double> = mutableMapOf()

    // The identity distinguishes the per-zone instances. reload_config_native_rate refreshes them;
    // dump_native_rate prints the same line again.
    override fun initialize() {
        _rates.putAll(_shopDataAccess.loadNativeRateConfig())
        _logger.log("[NativeRate] initialize #${System.identityHashCode(this)} -> ${dump()}")
    }

    override fun setConfig(rates: Map<DataType, Double>) {
        _rates.clear()
        _rates.putAll(rates)
    }

    override fun nativePerBcoin(dataType: DataType): Double? {
        return _rates[dataType]?.takeIf { it > 0 }
    }

    override fun nativePrice(dataType: DataType, bcoinPrice: Double): Double? {
        val rate = nativePerBcoin(dataType) ?: return null
        return bcoinPrice * rate
    }

    override fun dump(): String {
        if (_rates.isEmpty()) {
            return "config_native_rate: empty"
        }
        return _rates.entries
            .sortedBy { it.key.name }
            .joinToString(", ", prefix = "config_native_rate: ") { "${it.key.name}=${it.value}" }
    }
}
