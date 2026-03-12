package com.senspark.game.utils.random

import com.senspark.game.data.model.config.IHasWeightEntity

interface IWeightedRandom<T:IHasWeightEntity> {
    fun randomItem(): T
}