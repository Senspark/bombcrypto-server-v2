package com.senspark.game.manager.user

import com.senspark.game.api.IBlockchainDatabaseManager
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.manager.blockReward.IUserBlockRewardManager

class UserDataManager(
    private val _mediator: UserControllerMediator,
    private val blockRewardManager: IUserBlockRewardManager
) : IUserDataManager {

    private val databaseManager = _mediator.svServices.get<IBlockchainDatabaseManager>()
    private val userDataAccess = _mediator.services.get<IUserDataAccess>()
    
    override fun syncDeposited() {
        val userDeposited = databaseManager.depositedDatabase.query(_mediator.userId, _mediator.userName, _mediator.dataType)
        userDataAccess.syncDeposit(_mediator.userId, _mediator.dataType, userDeposited)
        blockRewardManager.loadUserBlockReward()
    }

    override fun updateLogoutInfo() {
        userDataAccess.updateLogoutInfo(_mediator.userId, _mediator.deviceType)
    }
    
    override fun syncDepositedV3() {
        try {
            databaseManager.depositedDatabase.queryV3(_mediator.userId, _mediator.userName, _mediator.dataType)
        } catch (e: Exception) {
            // Do nothing here
        }
    }
}