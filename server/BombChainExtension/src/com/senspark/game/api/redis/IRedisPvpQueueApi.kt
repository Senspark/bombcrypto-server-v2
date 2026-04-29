package com.senspark.game.api.redis

interface IPvpHeroInfo {
    /** Hero ID. */
    val heroId: Int

    /** Appearance stats. */
    val color: Int
    val skin: Int
    val skinChests: Map<Int, List<Int>>

    /** Base stats. */
    val health: Int
    val speed: Int
    val damage: Int
    val bombCount: Int
    val bombRange: Int
    val maxHealth: Int
    val maxSpeed: Int
    val maxDamage: Int
    val maxBombCount: Int
    val maxBombRange: Int
}

interface IPvpDataInfo {
    val serverId: String
    val matchId: String?
    val mode: Int
    val isBot: Boolean
    val displayName: String
    val totalMatchCount: Int // Used to determine whether user should play with bot.
    val rank: Int
    val point: Int
    val boosters: List<Int>
    val availableBoosters: Map<Int, Int>
    val hero: IPvpHeroInfo
    val avatar: Int?
    val wagerMode: Int
    val wagerTier: Int
    val wagerToken: Int
}

interface IPvpData {
    val userName : String
    val pings : Map<String, Int>
    val data : IPvpDataInfo
    val timestamp : Long?
    val newServer: Boolean
}

interface IRedisPvpQueueApi {
    fun joinQueue(info: PvpData);
    fun leaveQueue(username: String): Boolean
}