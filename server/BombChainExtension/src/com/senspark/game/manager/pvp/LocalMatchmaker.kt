package com.senspark.game.manager.pvp

import com.senspark.game.api.IPvpJoinQueueInfo
import com.senspark.game.controller.LegacyUserController
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException

class LocalMatchmaker : IMatchmaker {
    private class QueueEntry(
        val info: IPvpJoinQueueInfo,
    )

    private val _queueInfoLocker = Any()
    private val _queueInfoMap = mutableMapOf<String, QueueEntry>()
    override fun join(info: IPvpJoinQueueInfo): Boolean {
        val entry = QueueEntry(
            info = info,
        )
        synchronized(_queueInfoLocker) {
            // Check if already joined.
            if (_queueInfoMap.containsKey(info.username)) {
                throw CustomException("User has joined already", ErrorCode.PVP_ALREADY_IN_QUEUE)
            }
            // Add to queue.
            _queueInfoMap[info.username] = entry
            return true
        }
    }

    override fun keepJoining(username: String) {
        // No-op.
    }

    override fun leave(username: String): Boolean {
        val result: Boolean
        synchronized(_queueInfoLocker) {
            val item = _queueInfoMap.remove(username)
            result = item != null
        }
        return result
    }

    private fun handleTestJoin(controller: LegacyUserController): Boolean {
        // FIXME: join test match maker.
        /*
        val info = controller.pvPMapUser.info
        val joinQueueInfo = PvpJoinQueueInfo(
            controller.userName,
            controller.pvpHeroId,
            controller.pvpRank.point.value,
            _serverId,
            emptyMap(),
            info,
        )

        if (controller.masterUserManager.userConfigManager.pvpMatchCount < MIN_MATCH_COUNT_CAN_JOIN_QUEUE) {
            playPVPWithBot(controller, joinQueueInfo)
            return true
        }
        _testInfoMap[controller] = joinQueueInfo
        if (_testInfoMap.size == 2) {
            val pvpMapUserDataListJson = _testInfoMap.map { it.value.info }.toList().serialize()
            val matchToken = UUID.randomUUID().toString()
            val testServer = "192.168.1.104"
            _testInfoMap.forEach {
                onMatchFound(it.key, matchToken, testServer, pvpMapUserDataListJson, true)
            }
        }
        _testInfoMap.clear()
        */
        return true
    }
}