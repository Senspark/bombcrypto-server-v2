package com.senspark.game.db

import com.senspark.common.service.IGlobalService
import com.senspark.game.data.model.config.AirDrop
import com.senspark.game.data.model.user.AddUserItemWrapper
import com.senspark.game.db.model.NativeLedger
import com.senspark.game.db.model.NativePendingAccount
import com.senspark.game.db.model.NativeWithdrawRequest
import com.senspark.game.db.model.UserAirdropClaimed
import com.senspark.game.db.model.UserStakeInfo
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.customTypeAlias.ProductId
import kotlinx.serialization.json.JsonObject
import java.math.BigDecimal
import java.sql.SQLException

interface IRewardDataAccess : IGlobalService {
    fun addUserBlockReward(
        uid: Int,
        rewardType: BLOCK_REWARD_TYPE,
        dataType: DataType,
        value: Float,
        forControlValue: Float = 0f,
        reason: String
    )

    /**
     * Cho user stake bcoin
     * @param amount lượng bcoin stake
     * @return total token staked
     */
    @Throws(SQLException::class)
    fun userStake(
        username: String,
        dataType: DataType,
        blockRewardType: BLOCK_REWARD_TYPE,
        amount: Float,
        allIn: Boolean
    ): Double

    /**
     * withdraw coin staked
     * @param isWithDraw boolean => true (withdraw và cộng coin) false (chỉ tính toán lượng coin có thê rút, không cộng trừ coin
     * @return số coin withdraw được
     */
    @Throws(Exception::class)
    fun userWithdrawStake(
        dataType: DataType,
        username: String,
        isWithDraw: Boolean
    ): UserStakeInfo

    fun loadUserAirdropClaimed(uid: Int): HashMap<String, UserAirdropClaimed>
    fun userClaimAirdrop(
        uid: Int,
        airdrop: AirDrop,
        claimFee: Float,
        feeRewardType: BLOCK_REWARD_TYPE,
        isComplete: Int = 0
    )

    fun setUserClaimAirdropSuccess(uid: Int, airDropCodes: List<String>)
    fun saveUserClaimRewardData(
        uid: Int,
        dataType: DataType,
        rewardType: BLOCK_REWARD_TYPE,
        minClaim: Float,
        apiSyncedValue: Double,
    ): JsonObject

    fun syncUserClaimSynced(
        uid: Int,
        dataType: DataType,
        rewardType: BLOCK_REWARD_TYPE,
        apiSyncedValue: Double,
    ): Double

    fun subUserGem(uid: Int, amount: Float): Map<String, Float>

    /**
     * add all type reward for user (block reward, item)
     */
    fun addTRRewardForUser(
        uid: Int,
        dataType: DataType,
        rewardReceives: List<AddUserItemWrapper>,
        reloadRewardAfterAdd: () -> Unit,
        source: String = "unknown",
        rewardSpent: Map<BLOCK_REWARD_TYPE, Float> = emptyMap(),
        additionUpdateQueries: List<Pair<String, Array<Any?>>> = emptyList()
    )

    fun subUserReward(uid: Int, rewardType: BLOCK_REWARD_TYPE, value: Float, dataType: DataType, reason: String)
    fun checkBillTokenExist(billToken: String): Boolean

    // --- Cross-chain deposit bridge (see cross-chain-balance-impl-plan.md §D). ---
    // Under Option 2 (server-submitted withdraw) all fn_bridge_* mutations live in ap-deposit-bridge;
    // SmartFox only reads open pendings (to reflect a resume). reward_type = BCOIN_BRIDGE|SEN_BRIDGE.

    /** Open pending withdraws for a user (reward_type, chain, gross, before_value as wei strings). */
    fun loadCrosschainDepositBridgeOpenPendings(uid: Int): List<BridgeOpenPending>

    // --- Native (BNB / POL) deposit. --- Wei row is the sole money authority; user_block_reward
    // doubles are its projection. Caller reads on-chain counters first, these run the row-locked merge.

    /** Merge on-chain counters, settle any landed pending, re-project the doubles. Login + reconciler. */
    fun syncNativeDeposit(
        uid: Int,
        network: String,
        depositedWei: String,
        withdrawnWei: String,
        syncedBlock: Long?,
    ): NativeLedger

    /** Merge+settle, then re-sign the live pending or commit fresh spendable. Returns the cumulative to sign. */
    fun requestNativeWithdraw(
        uid: Int,
        network: String,
        depositedWei: String,
        withdrawnWei: String,
        syncedBlock: Long?,
    ): NativeWithdrawRequest

    /**
     * (uid, network) rows with pending_wei > 0 whose signature was issued within [windowSeconds],
     * oldest first, capped at [limit] — the bounded set the reconciler settles.
     */
    fun loadNativePendingAccounts(windowSeconds: Int, limit: Int): List<NativePendingAccount>
}

/** An open cross-chain bridge withdraw pending as raw DB strings (wei as integer strings). */
data class BridgeOpenPending(
    val rewardType: String,
    val chain: String,
    val grossWei: String,
    val beforeWei: String,
)