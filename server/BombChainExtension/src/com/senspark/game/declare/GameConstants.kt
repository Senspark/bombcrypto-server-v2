package com.senspark.game.declare

object GameConstants {
    const val ZONE_NAME: String = "BomberGameZone"
    const val MAP_MAX_COL: Int = 35
    const val MAP_MAX_ROW: Int = 17
    const val CHECK_DDOS_SUPPLY: Int = 100
    const val CHECK_DDOS_DURATION: Int = 15000
    const val BOMB_EXPLODE_DURATION: Int = 3000

    const val STORY_HERO_REST: Int = 86400000
    const val PLAY_STORY_REQUIRED_HERO: Int = 15
    const val VERIFY_SUPPLY: Int = 30
    const val DEFAULT_HASH: String = ""
    const val MAX_REWARD_TRIAL: Double = 5.0
    const val ENERGY_DANGEROUS: Int = 6
    const val MAX_RANK_STORY_HUNTER: Int = 501
    const val SQL_DATE_FORMAT: String = "yyyy-MM-dd"

    val staminaDame: Int
        get() = 160

    object BLOCK_TYPE {
        const val ROCK: Int = 0
        const val NORMAL: Int = 1
        const val JAIL: Int = 2
        const val WOODEN: Int = 3
        const val SILVER: Int = 4
        const val GOLDEN: Int = 5
        const val DIAMON: Int = 6
        const val LEGEND: Int = 7
    }

    object BOMBER_ABILITY {
        const val TREASURE_HUNTER: Int = 1
        const val JAIL_BREAKER: Int = 2
        const val PIERCE_BLOCK: Int = 3
        const val SAVE_BATTERY: Int = 4
        const val FAST_CHARGE: Int = 5
        const val BOMB_PASS: Int = 6
        const val BLOCK_PASS: Int = 7
        const val AVOID_THUNDER: Int = 1
    }

    object BOMBER_STAGE {
        const val WORK: Int = 0
        const val SLEEP: Int = 1
        const val HOUSE: Int = 2
    }

    object LOG_HACK_TYPE {
        const val HACK_SPEED: Int = 1
        const val HACK_EXPLODE_BLOCK: Int = 2
        const val HACK_STORY: Int = 3
    }

    object STORY_TYPE {
        const val STORY_MODE: Int = 0
    }

    object SCHEDULE_STATUS {
        const val PVP: String = "PVP"
    }

    object MARKETPLACE_STATUS {
        //status nft
        const val NORMAL: Int = 0
        const val LOCKED: Int = 1
        const val SELL: Int = 2
    }
}