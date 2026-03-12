package com.senspark.game.manager.onBoarding

import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.manager.IMasterDataManager
import com.senspark.game.db.IUserDataAccess
import com.smartfoxserver.v2.entities.data.ISFSObject

class UserOnBoardingManager(
    private val _mediator: UserControllerMediator,
) : IUserOnBoardingManager {
    private val _masterDataManager = _mediator.svServices.get<IMasterDataManager>()
    private val _userDataAccess = _mediator.services.get<IUserDataAccess>()

    private var _config: Map<Int, Float> = _masterDataManager.getOnBoardingConfig()
    private var _currentStep :Int = 0
    private var _currentClaimed: Int = 0;

    override fun getConfig(): Map<Int, Float> {
        return _config
    }

    override fun getUserProgress(userId: Int): Int {
        val result = _userDataAccess.getUserOnBoardingProgress(userId)
        _currentStep = result.getInt("step") ?: 0
        _currentClaimed = result.getInt("claimed") ?: 0
        return _currentStep
    }

    override fun updateUserProgress(userProgress: UserProgress) {
        if (!checkValidProgressData(userProgress)) {
            throw Exception("Invalid progress value")
        }
        
        userProgress.currentStep = _currentStep
        userProgress.currentClaimed = _currentClaimed
        userProgress.newClaimed = maxOf(userProgress.newClaimed, _currentClaimed)
        _currentClaimed = userProgress.newClaimed
        _userDataAccess.updateUserOnBoardingProgress(userProgress)
    }
    
    private fun checkValidProgressData(userProgress: UserProgress): Boolean{
        return userProgress.newStep > 0 &&
            userProgress.newClaimed >= 0 &&
            userProgress.newStep > _currentStep
    }
}
data class UserProgress(
    val userId: Int,
    val newStep: Int,
    var newClaimed: Int,
    var currentStep: Int,
    var currentClaimed: Int,
    val network: String,
    val rewardType: String
)