package com.senspark.game.data.model.nft

interface IHeroShieldBuilder {
    fun create(rarity: Int): HeroShield
    fun create(details: IHeroDetails): HeroShield
    fun fromString(rarity: Int, str: String, shieldLevel: Int): HeroShield
    fun getMaxCapacity(shieldLevel: Int, rarity: Int): Int
}