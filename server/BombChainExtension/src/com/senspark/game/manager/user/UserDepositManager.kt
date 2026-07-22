package com.senspark.game.manager.user

import com.senspark.game.api.IBlockchainDatabaseManager
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.extension.coroutines.ICoroutineScope
import com.senspark.game.manager.blockReward.IUserBlockRewardManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserDepositManager(
    private val _mediator: UserControllerMediator,
    private val blockRewardManager: IUserBlockRewardManager
) : IUserDepositManager {

    private val databaseManager = _mediator.svServices.get<IBlockchainDatabaseManager>()
    private val userDataAccess = _mediator.services.get<IUserDataAccess>()
    private val _coroutine = _mediator.services.get<ICoroutineScope>()
    
    override fun syncDeposited() {
        if (_mediator.walletAddress.isNullOrEmpty()) return
        val userDeposited = databaseManager.depositedDatabase.query(_mediator.userId, _mediator.walletAddress, _mediator.dataType)
        userDataAccess.syncDeposit(_mediator.userId, _mediator.dataType, userDeposited)
        blockRewardManager.loadUserBlockReward()
    }

    override fun syncDepositedV3() {
        if (_mediator.walletAddress.isNullOrEmpty()) return
        try {
            databaseManager.depositedDatabase.queryV3(_mediator.userId, _mediator.walletAddress, _mediator.dataType)
        } catch (e: Exception) {
            // Do nothing here
        }
    }

    // V4: fire-and-forget the blockchain API call.
    // Fresh data arrives to the client later via Redis stream AP_BL_SYNC_DEPOSIT → SYNC_DEPOSIT_RESPONSE push.
    override fun syncDepositedV4() {
        if (_mediator.walletAddress.isNullOrEmpty()) return
        val userId = _mediator.userId
        val wallet = _mediator.walletAddress
        val dataType = _mediator.dataType
        _coroutine.scope.launch(Dispatchers.IO) {
            databaseManager.depositedDatabase.queryV4(userId, wallet, dataType)
        }
    }
}