package com.senspark.game.pvp.entity

import com.senspark.game.constant.Booster

enum class HeroDamageSource {
    Null,
    Bomb,
    HardBlock,
    PrisonBreak,
}

enum class HeroItem {
    // Boosters.
    BombUp,
    FireUp,
    Boots,

    // Rewards.
    Gold,
    BronzeChest,
    SilverChest,
    GoldChest,
    PlatinumChest,
}

enum class HeroEffect {
    Shield,
    Invincible,
    Imprisoned,

    // Skull effects.
    SpeedTo1,
    SpeedTo10,
    ReverseDirection,
    PlantBombRepeatedly,
}

enum class HeroEffectReason {
    Null,

    /** Uses booster. */
    UseBooster,

    /** Takes item. */
    TakeItem,

    /** Effect time-out. */
    TimeOut,

    /** Broken by bomb damage. */
    Damaged,

    /** Rescued by teammate. */
    Rescue,
}

interface IHeroListener {
    fun onDamaged(hero: IHero, amount: Int, source: HeroDamageSource)
    fun onHealthChanged(hero: IHero, amount: Int, oldAmount: Int)
    fun onItemChanged(hero: IHero, item: HeroItem, amount: Int, oldAmount: Int)
    fun onEffectBegan(hero: IHero, effect: HeroEffect, reason: HeroEffectReason, duration: Int)
    fun onEffectEnded(hero: IHero, effect: HeroEffect, reason: HeroEffectReason)
    fun onMoved(hero: IHero, x: Float, y: Float)
}

interface IHero : IEntity {
    val state: IHeroState

    /** Owner ID, in range [0, playerCount). */
    val slot: Int

    val teamId: Int

    val damageSource: HeroDamageSource
    val items: Map<HeroItem, Int>
    val collectedItems: List<Int>
    val deathTimestamp: Long

    /** Vertical position. */
    val x: Float

    /** Horizontal position. */
    val y: Float

    /** Currently facing direction. */
    val direction: Direction

    /** Applies the specified state. */
    fun applyState(state: IHeroState)

    /**
     * Moves to the specified position.
     * @param x Horizontal position.
     * @param y Vertical position.
     */
    fun move(timestamp: Int, x: Float, y: Float)

    /**
     * Plants a bomb at the current position
     */
    fun plantBomb(timestamp: Int, byHero: Boolean): IBomb

    /**
     * Damages this hero by bomb.
     * @param amount Amount of damage.
     */
    fun damageBomb(amount: Int)

    /**
     * Damages this hero by prison break.
     */
    fun damagePrison()

    fun rescuePrison()

    /**
     * Damages this hero by falling blocks.
     */
    fun damageFallingBlock()

    /**
     * Uses the specified booster item.
     * @param booster The desired booster.
     */
    fun useBooster(booster: Booster)

    /**
     * Takes the specified item.
     */
    fun takeItem(blockType: BlockType)
}