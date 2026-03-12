package com.senspark.game.data.manager.grindHero

import com.senspark.game.constant.ItemKind
import com.senspark.game.data.model.config.GrindHero
import com.senspark.game.data.model.config.HeroGrindDropItem
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray

class NullGrindHeroManager : IGrindHeroManager {

    override fun initialize() {
    }
    
    override fun toSfsArray(): ISFSArray {
        return SFSArray()
    }

    override fun grind(itemKind: ItemKind, quantity: Int): Pair<GrindHero, MutableList<HeroGrindDropItem>> {
        throw CustomException("Feature not support")
    }
}