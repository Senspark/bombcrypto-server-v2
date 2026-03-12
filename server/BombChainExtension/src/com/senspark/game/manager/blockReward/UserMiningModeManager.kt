package com.senspark.game.manager.blockReward

import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.TokenType
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException

class UserMiningModeManager(
    private val _mediator: UserControllerMediator,
) : IUserMiningModeManager {

//    private val dataAccessManager: IDataAccessManager
    
    override val miningMode: TokenType
        get() = if (_mediator.dataType == EnumConstants.DataType.BSC) TokenType.COIN else TokenType.BCOIN

    override fun changeMiningMode(tokenType: TokenType) {
        throw CustomException("Not supported anymore", ErrorCode.SERVER_ERROR)
//        if (_mediator.userType != EnumConstants.UserType.FI || _mediator.dataType == EnumConstants.DataType.BSC) {
//            throw CustomException("Cannot change mode", ErrorCode.INVALID_PARAMETER)
//        }
//
//        _mediator.miningMode = tokenType
//        dataAccessManager.userDataAccess.updateMiningMode(_mediator.userId, tokenType)
    }
}