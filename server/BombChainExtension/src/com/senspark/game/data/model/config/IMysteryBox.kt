package com.senspark.game.data.model.config

interface IMysteryBox : IHasWeightEntity {
    val item: Item
    val quantity: Int
    val expirationAfter: Long?
}