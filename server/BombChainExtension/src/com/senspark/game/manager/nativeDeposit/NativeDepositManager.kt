package com.senspark.game.manager.nativeDeposit

import com.senspark.common.utils.ILogger
import com.senspark.game.db.IRewardDataAccess
import com.senspark.game.db.model.NativeLedger
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.SFSField
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class NativeDepositManager(
    private val _signer: INativeDepositResponseManager,
    private val _rewardDataAccess: IRewardDataAccess,
    private val _logger: ILogger,
) : INativeDepositManager {

    companion object {
        // A withdraw signature carries deadline = now + this (ap-deposit-native NativeConfig
        // signWindowSeconds). Past it DepositNative.withdraw() fails its `block.timestamp <= deadline`
        // check, so the pending can no longer land.
        private const val SIGN_WINDOW_SECONDS = 300

        // Counters are read at confirmed depth (BSC 5 blocks / POL 15), so a tx landing in the final
        // second of the window is only observable slightly after the deadline. This margin is exactly
        // that observation lag — nothing more, since past it the row cannot change until the user acts
        // again, and a fresh request or a login syncs on its own.
        private const val OBSERVE_MARGIN_SECONDS = 60

        private const val RECONCILE_WINDOW_SECONDS = SIGN_WINDOW_SECONDS + OBSERVE_MARGIN_SECONDS

        // Safety valve, not a tuning knob: the scheduler runs on one shared thread, so a pathological
        // burst must not make a single tick long enough to stall TH-mode rewards or PvP ranking.
        // Deferred rows are picked up on the next tick; hitting it is logged.
        private const val RECONCILE_LIMIT = 100
    }

    override fun initialize() {}

    // The game server only names the network; ap-deposit-native owns the address / chainId / deadline.
    private fun networkOf(rewardType: BLOCK_REWARD_TYPE): String {
        return when (rewardType) {
            BLOCK_REWARD_TYPE.BNB_DEPOSITED -> "BSC"
            BLOCK_REWARD_TYPE.POL_DEPOSITED -> "POLYGON"
            else -> throw CustomException("Invalid native token ${rewardType.name}")
        }
    }

    // Exact wei counters (deposited, withdrawn) from DepositNative, read at confirmed depth by the API.
    private suspend fun readCounters(uid: Int, walletAddress: String, network: String): Pair<String, String> {
        val result = _signer.requestCounters(uid, walletAddress, network)
        val deposited = result.deposited
        val withdrawn = result.withdrawn
        if (deposited == null || withdrawn == null) {
            _logger.error("[native] counters reply missing a field uid=$uid net=$network cid=${result.correlationId}")
            throw CustomException("Unable to reach the signer right now. Please try again later.")
        }
        return Pair(deposited, withdrawn)
    }

    override suspend fun sync(uid: Int, walletAddress: String, rewardType: BLOCK_REWARD_TYPE): NativeLedger {
        val network = networkOf(rewardType)
        val (deposited, withdrawn) = readCounters(uid, walletAddress, network)
        return _rewardDataAccess.syncNativeDeposit(uid, network, deposited, withdrawn, null)
    }

    override suspend fun requestWithdraw(uid: Int, walletAddress: String, rewardType: BLOCK_REWARD_TYPE): ISFSObject {
        val network = networkOf(rewardType)
        val (deposited, withdrawn) = readCounters(uid, walletAddress, network)

        val req = try {
            _rewardDataAccess.requestNativeWithdraw(uid, network, deposited, withdrawn, null)
        } catch (e: Exception) {
            _logger.error("[native-withdraw] request failed uid=$uid net=$network: ${e.message}", e)
            if (e.message?.contains("nothing to withdraw", ignoreCase = true) == true) {
                throw CustomException("You have nothing to withdraw.")
            }
            throw CustomException("Unable to process your withdraw right now. Please try again later.")
        }

        // ap-deposit-native stamps the deadline and returns the contract address + chainId to relay.
        val signed = _signer.requestWithdrawSign(uid, walletAddress, network, req.allowedCumulativeWei)
        val signature = signed.signature
        val deadline = signed.deadline
        val contractAddress = signed.contractAddress
        val chainId = signed.chainId
        if (signature.isNullOrEmpty() || deadline == null || contractAddress.isNullOrEmpty() || chainId == null) {
            // The pending is already committed at this point (rule 1) — the user re-requests and takes the
            // re-sign branch, so nothing is stranded.
            _logger.error("[native-withdraw] sign reply missing a field uid=$uid net=$network cid=${signed.correlationId}")
            throw CustomException("Unable to process your withdraw right now. Please try again later.")
        }

        val result: ISFSObject = SFSObject()
        result.putInt("code", 0)
        result.putUtfString(SFSField.signature, signature)
        result.putUtfString(SFSField.ALLOWED_CUMULATIVE, req.allowedCumulativeWei)
        result.putLong(SFSField.DEADLINE, deadline)
        result.putUtfString(SFSField.CONTRACT_ADDRESS, contractAddress)
        result.putLong(SFSField.CHAIN_ID, chainId.toLong())
        result.putUtfString(SFSField.CHAIN, network)
        return result
    }

    override suspend fun reconcilePending() {
        val pendings = try {
            _rewardDataAccess.loadNativePendingAccounts(RECONCILE_WINDOW_SECONDS, RECONCILE_LIMIT)
        } catch (e: Exception) {
            _logger.error("[native-reconcile] load pending failed: ${e.message}", e)
            return
        }
        if (pendings.isEmpty()) {
            return
        }
        // Sequential on purpose: one counter read in flight at a time keeps the RPC provider's rate
        // limit out of play. The window above is what keeps this list short, not concurrency.
        var settled = 0
        var failed = 0
        for (p in pendings) {
            try {
                val (deposited, withdrawn) = readCounters(p.uid, p.wallet, p.network)
                val ledger = _rewardDataAccess.syncNativeDeposit(p.uid, p.network, deposited, withdrawn, null)
                if (ledger.pendingWei.toBigIntegerOrNull()?.signum() == 0) {
                    settled++
                }
            } catch (e: Exception) {
                failed++
                _logger.error("[native-reconcile] uid=${p.uid} net=${p.network} failed: ${e.message}", e)
            }
        }
        _logger.log("[native-reconcile] scanned=${pendings.size} settled=$settled failed=$failed")
        if (pendings.size >= RECONCILE_LIMIT) {
            // Sustained, this means the window holds more withdrawals than one tick can read, and the
            // deferred rows may age out of it unread. Raise the limit, or move to a log sweep.
            _logger.warn("[native-reconcile] hit limit $RECONCILE_LIMIT, remaining rows deferred to the next tick")
        }
    }

}
