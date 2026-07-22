package com.senspark.game.manager.treasureHuntV2

import com.senspark.common.utils.ILogger
import com.senspark.game.controller.IUserController
import com.smartfoxserver.v2.api.CreateRoomSettings
import com.smartfoxserver.v2.entities.Room
import com.smartfoxserver.v2.entities.SFSRoomSettings
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable
import com.smartfoxserver.v2.extensions.SFSExtension
import java.util.*

class THModeV2Room(
    private val period: Int,
    private val maxPool: ISFSArray,
    private val _logger: ILogger,
    extension: SFSExtension
) : ITHModeV2Room {
    companion object {
        const val ROOM_NAME = "THModeV2"
        const val V_NEVER_CHANGED = "never_changed"
        const val V_TIME = "time"
        const val V_POOL_BCOIN = "pool_bcoin"

        const val V_PERIOD = "period"
        const val V_MAX_POOL = "max_pool"
        const val V_NEXT_TIME_REFILL_POOL = "next_time_refill_pool"
        const val V_REMAINING_POOL_C = "remaining_pool_common"
        const val V_REMAINING_POOL_R = "remaining_pool_rare"
        const val V_REMAINING_POOL_SR = "remaining_pool_super_rare"
        const val V_REMAINING_POOL_E = "remaining_pool_epic"
        const val V_REMAINING_POOL_L = "remaining_pool_legend"
        const val V_REMAINING_POOL_SL = "remaining_pool_super_legend"
        const val V_REMAINING_POOL_MEGA = "remaining_pool_mega"
        const val V_REMAINING_POOL_SUPER_MEGA = "remaining_pool_super_mega"
        const val V_REMAINING_POOL_MYSTIC = "remaining_pool_mystic"
        const val V_REMAINING_POOL_SUPER_MYSTIC = "remaining_pool_super_mystic"
    }

    private val _api = extension.api

    private val _room: Room
    private val _roomVariables: MutableMap<String, Any> = mutableMapOf()

    init {
        val settings = CreateRoomSettings()
        settings.name = ROOM_NAME
        settings.maxUsers = 10_000

        // 4 config vars (V_NEVER_CHANGED, V_PERIOD, V_MAX_POOL, V_NEXT_TIME_REFILL_POOL)
        // + 10 pool vars (V_REMAINING_POOL_C..SUPER_MYSTIC) = 14 needed; 20 for future headroom.
        settings.maxVariablesAllowed = 20
        settings.roomSettings = setOf(
            SFSRoomSettings.USER_VARIABLES_UPDATE_EVENT,
            SFSRoomSettings.USER_ENTER_EVENT,
            SFSRoomSettings.ROOM_NAME_CHANGE
        )

        // thời gian cho lần refill pool tiếp theo
        val now = Calendar.getInstance()
        val timeNextRefill = Calendar.getInstance()
        timeNextRefill.set(Calendar.HOUR_OF_DAY, 7)
        timeNextRefill.set(Calendar.MINUTE, 0)
        timeNextRefill.set(Calendar.SECOND, 10)
        timeNextRefill.set(Calendar.MILLISECOND, 0)
        if (now.after(timeNextRefill)) {
            timeNextRefill.add(Calendar.DAY_OF_MONTH, 1)
        }
        settings.roomVariables = listOf(
            SFSRoomVariable(V_NEVER_CHANGED, "hello"),
            SFSRoomVariable(V_PERIOD, period),
            SFSRoomVariable(V_MAX_POOL, maxPool), //For old client
            SFSRoomVariable(V_NEXT_TIME_REFILL_POOL, timeNextRefill.toInstant().epochSecond),
        )

        _room = extension.parentZone.createRoom(settings)
    }

    override fun joinRoom(controller: IUserController) {
        val user = controller.user ?: return
        if (_room.containsUser(user)) {
            return
        }
        try {
            _api.joinRoom(user, _room)
        } catch (e: Exception) {
            _logger.log("$ROOM_NAME Error: ${e.message}")
        }
    }

    override fun leaveRoom(controller: IUserController) {
        val user = controller.user ?: return
        if (!_room.containsUser(user)) {
            return
        }
        try {
            _api.leaveRoom(user, _room)
        } catch (e: Exception) {
            _logger.log("$ROOM_NAME Error: ${e.message}")
        }
    }

    override fun poolIdToKey(poolId: Int): String {
        return when (poolId) {
            0 -> V_REMAINING_POOL_C
            1 -> V_REMAINING_POOL_R
            2 -> V_REMAINING_POOL_SR
            3 -> V_REMAINING_POOL_E
            4 -> V_REMAINING_POOL_L
            5 -> V_REMAINING_POOL_SL
            6 -> V_REMAINING_POOL_MEGA
            7 -> V_REMAINING_POOL_SUPER_MEGA
            8 -> V_REMAINING_POOL_MYSTIC
            9 -> V_REMAINING_POOL_SUPER_MYSTIC
            else -> throw IllegalArgumentException("Invalid TH Mode poolId: $poolId (expected 0..9)")
        }
    }
    

    override fun updateRewardPoolVariable(remainingPool: ISFSArray) {
        val changeVariables: MutableMap<String, Any> = mutableMapOf()
        for (i in 0 until remainingPool.size()) {
            val rewardPoolById = remainingPool.getSFSObject(i)
            val poolId = rewardPoolById.getInt("pool_id")
            val rewardByType = rewardPoolById.getSFSArray("reward_by_type")

            val keyPool = poolIdToKey(poolId)
            if (!_roomVariables.containsKey(keyPool)) {
                changeVariables[keyPool] = rewardByType
                _roomVariables[keyPool] = rewardByType
                continue
            }
            val rewardPoolInRoom: SFSArray = _roomVariables[poolIdToKey(i)] as SFSArray
            //Kiểm tra có thay đổi giá trị không
            val rewardPoolBcoinRoom = rewardPoolInRoom.getSFSObject(0).getDouble("remaining_reward").toInt()
            val rewardPoolBcoin = rewardByType.getSFSObject(0).getDouble("remaining_reward").toInt()
            if (rewardPoolBcoinRoom == rewardPoolBcoin) {
                continue
            }

            changeVariables[keyPool] = rewardByType
            _roomVariables[keyPool] = rewardByType
        }

        val roomVariables = changeVariables.map { SFSRoomVariable(it.key, it.value) }.toList()
        _api.setRoomVariables(null, _room, roomVariables)
    }

    override fun updateConfigVariable(period: Int, maxPool: ISFSArray) {
        val changeVariables: MutableMap<String, Any> = mutableMapOf()

        changeVariables[V_PERIOD] = period
        changeVariables[V_MAX_POOL] = maxPool

        val roomVariables = changeVariables.map { SFSRoomVariable(it.key, it.value) }.toList()
        _api.setRoomVariables(null, _room, roomVariables)
    }

    override fun updateTimeRefillPoolVariable(time: Long) {
        val changeVariables: MutableMap<String, Any> = mutableMapOf()

        changeVariables[V_NEXT_TIME_REFILL_POOL] = time

        val roomVariables = changeVariables.map { SFSRoomVariable(it.key, it.value) }.toList()
        _api.setRoomVariables(null, _room, roomVariables)
    }
}