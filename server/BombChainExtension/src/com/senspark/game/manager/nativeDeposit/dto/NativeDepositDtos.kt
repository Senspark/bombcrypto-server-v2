package com.senspark.game.manager.nativeDeposit.dto

import kotlinx.serialization.Serializable

// --- ap-deposit-native Redis payloads (must match api/deposit-native Messages.ts). ---
// Wei quantities travel as decimal strings end-to-end: a 1-wei rounding breaks signature verification.

/** SV_DEPNATIVE_REQUEST_STR `kind` values. */
object NativeDepositRequestKind {
    const val COUNTERS = "counters"
    const val WITHDRAW_SIGN = "withdraw-sign"
}

/**
 * AP_DEPNATIVE_RESULT_STR — signer → server, matched by correlationId. `code` 0 ok / 100 error.
 *
 * A `counters` reply carries [deposited] / [withdrawn]; a `withdraw-sign` reply carries [signature] and
 * the parameters the user relays. Both shapes share one message so there is a single listener and a
 * single pending map.
 */
@Serializable
data class NativeDepositResult(
    val correlationId: String,
    val code: Int = 100,
    val errorMessage: String? = null,
    val deposited: String? = null,
    val withdrawn: String? = null,
    val signature: String? = null,
    val deadline: Long? = null,
    val contractAddress: String? = null,
    val chainId: Int? = null,
)
