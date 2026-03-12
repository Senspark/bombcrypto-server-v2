package com.senspark.game.service

import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.manager.hero.IUserHeroFiManager

class NullAllHeroesFiManager : IAllHeroesFiManager {
    override fun initialize() {
    }
    
    override fun processHeroStake(json: String) {}

    override fun addSubManager(userId: Int, dataType: DataType, heroFiManager: IUserHeroFiManager) {}

    override fun removeSubManager(userId: Int, dataType: DataType) {}

    override fun getSubManager(userId: Int, network: DataType): IUserHeroFiManager? {
        return null
    }

    override fun destroy() {}
}