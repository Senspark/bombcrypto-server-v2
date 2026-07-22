package com.senspark.game.manager.crosschainDepositBridge.dto

import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSObject
import kotlinx.serialization.Serializable

/**
 * A bridge-supported chain. `dbName` is the value sent to ap-deposit-bridge over Redis (BSC|POLYGON) —
 * the backend keys its network config on this exact value.
 */
enum class CrosschainDepositBridgeChain(val dbName: String) {
    BSC("BSC"),
    POLYGON("POLYGON"),
}

/**
 * The four bridge activities the client reports (SV_DEPBRIDGE_NOTIFY_STR `kind`). Must match
 * api/deposit-bridge Consts.ts NotifyKind. WITHDRAW_REQUEST is emitted server-side (the withdraw handler);
 * the other three come from the client (before/after deposit, after withdraw).
 */
object CrosschainDepositBridgeNotifyKind {
    const val DEPOSIT_PREPARE = "deposit_prepare"
    const val DEPOSIT_DONE = "deposit_done"
    const val WITHDRAW_REQUEST = "withdraw_request"
    const val WITHDRAW_DONE = "withdraw_done"

    // The kinds a client is allowed to report; WITHDRAW_REQUEST is server-only.
    val CLIENT_KINDS = setOf(DEPOSIT_PREPARE, DEPOSIT_DONE, WITHDRAW_DONE)
}

/** The login chain, used as the default withdraw target when the client doesn't pick one. */
fun crosschainDepositBridgeChainOf(dataType: DataType): CrosschainDepositBridgeChain = when (dataType) {
    DataType.BSC -> CrosschainDepositBridgeChain.BSC
    DataType.POLYGON -> CrosschainDepositBridgeChain.POLYGON
    else -> throw CustomException("Cross-chain bridge is not supported on ${dataType.name}")
}

/**
 * Parse the client-supplied withdraw network (SFSField.CHAIN) — the chain the user will relay the
 * withdraw ON. Case-insensitive; returns null when absent/unrecognized so the caller can fall back to
 * the login chain.
 */
fun crosschainDepositBridgeChainFromClientOrNull(raw: String?): CrosschainDepositBridgeChain? = when (raw?.uppercase()) {
    "BSC", "BNB" -> CrosschainDepositBridgeChain.BSC
    "POLYGON", "POL" -> CrosschainDepositBridgeChain.POLYGON
    else -> null
}

/**
 * Result of forwarding a withdraw-sign request to ap-deposit-bridge. [Ok] carries the success payload
 * (the signature + relay parameters) to send on the original request; [Rejected] carries a **client-safe**
 * message + code to send via `sendError`. Business errors are modelled here instead of thrown as
 * [CustomException] so no backend/RPC text can ever reach the client.
 */
sealed interface CrosschainDepositBridgeWithdrawOutcome {
    data class Ok(val payload: ISFSObject) : CrosschainDepositBridgeWithdrawOutcome
    data class Rejected(val code: Int, val safeMessage: String) : CrosschainDepositBridgeWithdrawOutcome
}

// --- ap-deposit-bridge Redis payloads (must match api/deposit-bridge Messages.ts). ---
// Wei quantities travel as strings end-to-end.

/**
 * AP_DEPBRIDGE_RESULT_STR — signer → server, matched by correlationId. `code` 0 ok / 100 error.
 * On success carries the EIP-191 signature + the parameters the user relays: otherDeposited (source-chain
 * deposited counter), deadline, and the withdraw-chain bridge/token addresses.
 */
@Serializable
data class CrosschainDepositBridgeResult(
    val correlationId: String,
    val code: Int = 100,
    val errorMessage: String? = null,
    val signature: String? = null,
    val otherDeposited: String? = null,
    val deadline: Long? = null,
    val bridgeAddress: String? = null,
    val tokenAddress: String? = null,
    val chain: String? = null,
)
