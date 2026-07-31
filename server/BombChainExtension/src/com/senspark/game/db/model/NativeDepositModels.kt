package com.senspark.game.db.model

// Wei-exact ledger snapshot returned by fn_native_sync (all strings — never float on the money path).
data class NativeLedger(
    val depositedWei: String,
    val withdrawnWei: String,
    val pendingWei: String,
    val spendableWei: String,
)

// Result of fn_native_request_withdraw: the cumulative to sign (exact wei), the locked pending, and
// whether this re-signed an existing pending (one-at-a-time) rather than committing fresh spendable.
data class NativeWithdrawRequest(
    val allowedCumulativeWei: String,
    val pendingWei: String,
    val reused: Boolean,
)

// A (uid, network) row with an outstanding withdraw, joined to its wallet — the bounded set the
// reconciler settles. Native pending rows are always FI users, so "user".user_name is the wallet.
data class NativePendingAccount(
    val uid: Int,
    val network: String,
    val wallet: String,
)
