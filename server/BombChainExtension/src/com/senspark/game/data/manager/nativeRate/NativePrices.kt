package com.senspark.game.data.manager.nativeRate

import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.DataType
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

/**
 * `prices[]` — one entry per reward type that can pay for an item, keyed by the wire int the client
 * echoes back when buying, so adding a currency later needs no shape change.
 */
fun pricesArray(vararg listed: Pair<BLOCK_REWARD_TYPE, Double>): ISFSArray {
    val result = SFSArray()
    listed.forEach { (rewardType, price) -> result.addSFSObject(priceEntry(rewardType, price)) }
    return result
}

/**
 * The native coin entry for [pricesArray], or null when the network has no native balance. Display
 * figure only — fn_native_price recomputes the charge at purchase time.
 */
fun INativeRateManager.nativePriceEntry(dataType: DataType, bcoinPrice: Double): ISFSObject? {
    val nativeType = dataType.convertToNativeDepositType() ?: return null
    val price = nativePrice(dataType, bcoinPrice) ?: return null
    return priceEntry(nativeType, price)
}

// `reward_type` is BLOCK_REWARD_TYPE.value, NOT the client's own BlockRewardType enum: they diverge
// past 28 (BNB_DEPOSITED is 31 here, 34 there). Client maps through LaunchPadTokenTable.code.
private fun priceEntry(rewardType: BLOCK_REWARD_TYPE, price: Double) = SFSObject().apply {
    putInt("reward_type", rewardType.value)
    putDouble("price", price)
}
