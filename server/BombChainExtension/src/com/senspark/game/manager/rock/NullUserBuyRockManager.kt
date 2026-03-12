package com.senspark.game.manager.rock

import com.senspark.game.annotation.FunctionTest
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.model.config.RockPackage
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException

class NullUserBuyRockManager : IUserBuyRockManager {

    @FunctionTest
    override fun buyPackage(rockPackage: RockPackage, blockRewardType: BLOCK_REWARD_TYPE) {
    }
}