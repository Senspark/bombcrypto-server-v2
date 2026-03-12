package com.senspark.game.manager.stake

import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.SFSField
import com.senspark.game.utils.Utils
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.SQLException

class UserStakeManager(
    mediator: UserControllerMediator
) : IUserStakeManager {
    private val _dataAccessManager = mediator.services.get<IDataAccessManager>()

    override fun stake(
        username: String,
        blockRewardType: BLOCK_REWARD_TYPE,
        amount: Float,
        allIN: Boolean
    ): SFSObject {
        try {
            val totalStaked = _dataAccessManager.rewardDataAccess.userStake(
                username,
                DataType.BSC,
                blockRewardType,
                amount,
                allIN
            )
            val result = SFSObject()
            result.putDouble(SFSField.TOTAL_STAKED, totalStaked)
            return result
        } catch (ex: SQLException) {
            throw Utils.parseSQLException(ex)
        }
    }

    override fun withdrawStake(username: String, isWithdraw: Boolean): SFSObject {
        try {
            val userStakeInfo =
                _dataAccessManager.rewardDataAccess.userWithdrawStake(DataType.BSC, username, isWithdraw)
            val result = SFSObject()
            result.putDouble(SFSField.PRINCIPAL, userStakeInfo.principal)
            result.putDouble(SFSField.PROFIT, userStakeInfo.profit)
            result.putLong(SFSField.STAKE_DATE, userStakeInfo.stakeDate)
            result.putDouble(SFSField.WITHDRAW_FEE, userStakeInfo.withdrawFee)
            result.putDouble(SFSField.TOTAL_STAKE, userStakeInfo.totalStake)
            result.putDouble(SFSField.APD, userStakeInfo.APD)
            result.putDouble(SFSField.RECEIVE_AMOUNT, userStakeInfo.receiveAmount)
            result.putDouble(SFSField.PRINCIPAL, userStakeInfo.principal)
            return result
        } catch (ex: SQLException) {
            throw Utils.parseSQLException(ex)
        } catch (ex: Exception) {
            throw ex
        }
    }
}