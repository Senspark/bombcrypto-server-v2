package com.senspark.game.manager.ton

import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.declare.EnumConstants
import com.senspark.game.exception.CustomException

class UserDepositedTransactionManager(mediator: UserControllerMediator) : IUserDepositedTransactionManager {
    private val _dataAccessManager = mediator.services.get<IDataAccessManager>()

    override fun createDepositedTransaction(uid: Int, dataType: EnumConstants.DataType, depositType: EnumConstants.DepositType): Int {
        //TON có Bcoin deposit và Ton deposit
        if (dataType == EnumConstants.DataType.TON) {
            if(depositType == EnumConstants.DepositType.BCOIN_DEPOSIT)
                return _dataAccessManager.userDataAccess.createTonTransaction(uid)
            else if(depositType == EnumConstants.DepositType.TON_DEPOSIT)
                return _dataAccessManager.userDataAccess.createTonTransaction(uid)
        }
        else if (dataType == EnumConstants.DataType.SOL)
            return _dataAccessManager.userDataAccess.createSolTransaction(uid)
            
        //RON chỉ có Ron deposit (không có BCOIN_DEPOSIT)
        else if (dataType == EnumConstants.DataType.RON) {
            if(depositType == EnumConstants.DepositType.RON_DEPOSIT)
                return _dataAccessManager.userDataAccess.createRonTransaction(uid)
            else
                throw CustomException("RON only supports RON_DEPOSIT")
        }

        else if (dataType == EnumConstants.DataType.BAS) {
            if(depositType == EnumConstants.DepositType.BAS_DEPOSIT)
                return _dataAccessManager.userDataAccess.createBasTransaction(uid)
            else
                throw CustomException("RON only supports RON_DEPOSIT")
        }

        else if (dataType == EnumConstants.DataType.VIC) {
            if(depositType == EnumConstants.DepositType.VIC_DEPOSIT)
                return _dataAccessManager.userDataAccess.createVicTransaction(uid)
            else
                throw CustomException("VIC only supports VIC_DEPOSIT")
        }
        
        throw CustomException("Not User Ton, Sol, Ron or Bas")
    }

    // Hiện ko dùng function này
    override fun updateDepositedTransaction(
        id: Int,
        amount: Double,
        txHash: String,
        token: String,
        dataType: EnumConstants.DataType,
        sender: String?
    ) {
        if (dataType == EnumConstants.DataType.TON)
            _dataAccessManager.userDataAccess.updateTonTransaction(id, amount, txHash, token)
        else if (dataType == EnumConstants.DataType.SOL)
            _dataAccessManager.userDataAccess.updateSolTransaction(id, amount, txHash, token)
        else if (dataType == EnumConstants.DataType.RON)
            _dataAccessManager.userDataAccess.updateRonTransaction(id, amount, txHash, token, sender ?: "")
        else if (dataType == EnumConstants.DataType.BAS)
            _dataAccessManager.userDataAccess.updateBasTransaction(id, amount, txHash, token, sender ?: "")
    }
}