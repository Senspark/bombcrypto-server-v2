package com.senspark.game.data.manager.grindHero

import com.senspark.common.service.IServerService
import com.senspark.game.constant.ItemKind
import com.senspark.game.data.model.config.GrindHero
import com.senspark.game.data.model.config.HeroGrindDropItem
import com.smartfoxserver.v2.entities.data.ISFSArray

interface IGrindHeroManager : IServerService {
    fun grind(itemKind: ItemKind, quantity: Int): Pair<GrindHero, MutableList<HeroGrindDropItem>>
    fun toSfsArray(): ISFSArray
} 