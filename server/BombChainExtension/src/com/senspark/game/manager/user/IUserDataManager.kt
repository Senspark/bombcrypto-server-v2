package com.senspark.game.manager.user

interface IUserDataManager {
    fun syncDeposited()
    fun syncDepositedV3()
    fun updateLogoutInfo()
}