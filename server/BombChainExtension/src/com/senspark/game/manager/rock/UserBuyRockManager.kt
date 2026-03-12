package com.senspark.game.manager.rock

import com.senspark.game.annotation.FunctionTest
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.model.config.RockPackage
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException

class UserBuyRockManager(
    private val _mediator: UserControllerMediator,
) : IUserBuyRockManager {

    private val userDataAccess = _mediator.services.get<IUserDataAccess>()

    @FunctionTest
    override fun buyPackage(rockPackage: RockPackage, blockRewardType: BLOCK_REWARD_TYPE) {
        val firstRewardType: String
        val secondRewardType: String
        when (blockRewardType) {
            BLOCK_REWARD_TYPE.BCOIN -> {
                firstRewardType = BLOCK_REWARD_TYPE.BCOIN_DEPOSITED.name
                secondRewardType = BLOCK_REWARD_TYPE.BCOIN.name
            }

            BLOCK_REWARD_TYPE.SENSPARK -> {
                firstRewardType = BLOCK_REWARD_TYPE.SENSPARK_DEPOSITED.name
                secondRewardType = BLOCK_REWARD_TYPE.SENSPARK.name
            }

            else -> throw CustomException("Reward type invalid", ErrorCode.INVALID_PARAMETER)
        }

        userDataAccess.buyRockPackage(
            _mediator.userId,
            rockPackage.getName(),
            _mediator.dataType.name,
            firstRewardType,
            secondRewardType
        )
    }

}