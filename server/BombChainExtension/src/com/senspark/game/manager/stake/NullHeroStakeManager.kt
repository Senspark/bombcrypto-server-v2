package com.senspark.game.manager.stake

import com.senspark.game.data.model.nft.Hero
import com.senspark.game.declare.EnumConstants

class NullHeroStakeManager : IHeroStakeManager {
    override val minStakeHeroConfig: Map<Int, Int> get() = emptyMap()

    override fun initialize() {
    }

    override fun setConfig(minStakeHeroList: Map<Int, Int>) {}

    override fun checkBomberStake(dataType: EnumConstants.DataType, bomber: Hero) {}

    override fun destroy() {}
}