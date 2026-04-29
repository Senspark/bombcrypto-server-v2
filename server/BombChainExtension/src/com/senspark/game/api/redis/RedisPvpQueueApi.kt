package com.senspark.game.api.redis

import com.senspark.common.cache.IMessengerService
import com.senspark.game.constant.StreamKeys
import com.senspark.game.pvp.utility.JsonUtility
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
class PvpHeroInfo(
    /** Hero ID. */
    override val heroId: Int,

    /** Appearance stats. */
    override val color: Int,
    override val skin: Int,
    override val skinChests: Map<Int, List<Int>>,

    /** Base stats. */
    override val health: Int,
    override val speed: Int,
    override val damage: Int,
    override val bombCount: Int,
    override val bombRange: Int,
    override val maxHealth: Int,
    override val maxSpeed: Int,
    override val maxDamage: Int,
    override val maxBombCount: Int,
    override val maxBombRange: Int
) : IPvpHeroInfo

@Serializable
class PvpDataInfo(
    override val serverId: String,
    override val matchId: String?,
    override val mode: Int,
    override val isBot: Boolean,
    override val displayName: String,
    override val totalMatchCount: Int, // Used to determine whether user should play with bot.
    override val rank: Int,
    override val point: Int,
    override val boosters: List<Int>,
    override val availableBoosters: Map<Int, Int>,
    override val hero: PvpHeroInfo,
    override val avatar: Int?,
    override val wagerMode: Int = 0,
    override val wagerTier: Int = 0,
    override val wagerToken: Int = 0,
    override val network: String = ""
) : IPvpDataInfo

@Serializable
class PvpData(
    override val userName: String,
    override val pings: Map<String, Int>,
    override val data: PvpDataInfo,
    override val timestamp: Long?,
    override val newServer: Boolean
) : IPvpData


class RedisPvpQueueApi(messengerService: IMessengerService) : IRedisPvpQueueApi {
    private val _json = JsonUtility.json
    private val _messengerService = messengerService
    private val _queue = mutableSetOf<String>()

    override fun joinQueue(info: PvpData) {
        _queue.add(info.userName)
        val requestJson = _json.encodeToString(info)
        _messengerService.send(StreamKeys.SV_GAME_JOIN_PVP_STR, requestJson)
    }

    override fun leaveQueue(username: String): Boolean {
        if (_queue.contains(username)) {
            _queue.remove(username)
            _messengerService.send(
                StreamKeys.SV_GAME_LEAVE_PVP_STR,
                _json.encodeToString(mapOf("userName" to username))
            )
        }

        // Gởi lệnh leaveQueue và xem như luôn luôn thành công.
        return true
    }
}