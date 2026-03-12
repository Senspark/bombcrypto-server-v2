package com.senspark.game.data.model.config

class HeroGrindDropItem(
    val item: Item,
    val quantity: Int,
    override val weight: Float
) : IHasWeightEntity