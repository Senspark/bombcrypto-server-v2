package com.senspark.game.manager.stake

import com.senspark.common.service.IServerService
import com.senspark.common.service.IService
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.declare.EnumConstants.DataType

interface IHeroStakeManager : IService, IServerService {
    val minStakeHeroConfig: Map<Int, Int>
    fun setConfig(minStakeHeroList: Map<Int, Int>)
    fun checkBomberStake(dataType: DataType, bomber: Hero)
}