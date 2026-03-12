package com.senspark.game.service

import com.senspark.common.service.IServerService
import com.senspark.common.service.IService
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.manager.hero.IUserHeroFiManager

/**
 * Quản lý tất cả các Hero Fi bên trong server
 */
interface IAllHeroesFiManager : IService, IServerService {
    fun processHeroStake(json: String)
    fun addSubManager(userId: Int, dataType: DataType, heroFiManager: IUserHeroFiManager)
    fun removeSubManager(userId: Int, dataType: DataType)
    fun getSubManager(userId: Int, network: DataType): IUserHeroFiManager?
}

