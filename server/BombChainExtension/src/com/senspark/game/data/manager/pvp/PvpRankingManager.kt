package com.senspark.game.data.manager.pvp

import com.senspark.common.pvp.IRankManager
import com.senspark.common.service.IServerService
import com.senspark.common.utils.ILogger
import com.senspark.game.controller.LegacyUserController
import com.senspark.game.data.manager.season.IPvpSeasonManager
import com.senspark.game.data.model.user.PvPRank
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IUsersManager
import com.senspark.game.service.IPvpDataAccess
import com.senspark.game.user.UserPoint
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray

interface IPvpRankingManager : IServerService {
    fun reload()
    fun getRanking(userId: Int): PvPRank?
    fun getRanking(name: String, userId: Int, dataType: DataType): PvPRank
    fun getTotalCount(): Int
    fun toSFSArray(): ISFSArray
    fun decayUserRank()
    fun getDecayPointUser(uid: Int): Int
    fun clearDataForOneUser(uid: Int)
}

class PvpRankingManager(
    private val _logger: ILogger,
    private val _envManager: IEnvManager,
    private val _pvpDataAccess: IPvpDataAccess,
    private val _userDataAccess: IUserDataAccess,
    private val _pvpSeasonManager: IPvpSeasonManager,
    private val _pvpRankManager: IRankManager,
    private val _usersManager: IUsersManager,
    private val _gameConfigManager: IGameConfigManager,
) : IPvpRankingManager {
    private lateinit var _data: MutableMap<Int, PvPRank>
    private lateinit var _avatarData: MutableMap<Int, Int>

    // lưu lại những user bị decay để hiện dialog
    private lateinit var _decayUsers: MutableMap<Int, Int>

    override fun initialize() {
        reload()
    }

    override fun reload() {
        _data = _pvpDataAccess.queryPvPRank(_pvpSeasonManager.currentSeasonNumber, _pvpRankManager)
        _avatarData = _userDataAccess.queryAllUserAvatarActive()

        // Thêm avatar để client hiển thị trong pvp ranking
        _data.values.forEach {
            it.avatar = _avatarData[it.uid] ?: -1
        }
    }

    override fun getRanking(userId: Int): PvPRank? {
        val rank = _data[userId]
        return rank
    }

    override fun getRanking(name: String, userId: Int, dataType: DataType): PvPRank {
        val defaultMatchCount = 0
        val defaultRank = 0
        val defaultWinMatchCount = 0
        var rank = getRanking(userId)
        if (rank == null) {
            rank = PvPRank(
                name,
                userId,
                defaultRank,
                UserPoint(
                    invokeReturnValue({
                        _pvpDataAccess.queryWinBonusPvPRankingPoint(
                            0,
                            _pvpSeasonManager.currentSeasonNumber,
                            userId,
                            dataType
                        )
                    }, 3) as Int
                ),
                defaultMatchCount,
                defaultWinMatchCount,
                _userDataAccess.queryUserAvatarActive(userId),
                _pvpRankManager
            )
        }
        return rank
    }

    override fun getTotalCount(): Int {
        return _data.size
    }

    override fun toSFSArray(): ISFSArray {
        val result = SFSArray()
        val dataSize = _gameConfigManager.sizeRankingLeaderboard
        _data.values.filter { it.rank <= dataSize && it.rank != 0 }.sortedBy { it.rank }.forEach {
            result.addSFSObject(it.toSFSObject())
        }
        return result
    }

    override fun decayUserRank() {
        _decayUsers = mutableMapOf()
        reload()
        val matchUsers = _pvpDataAccess.getAllAmountPvpMatchesCurrentDate(_pvpSeasonManager.currentSeasonNumber)
        val userPoint = mutableMapOf<Int, Int>()
        matchUsers.forEach { (uid, amountMatches) ->
            val point = _data[uid]?.point?.value ?: return@forEach
            val rankConfig = _pvpRankManager.getBombRank(point)
            if (amountMatches < rankConfig.minMatches) {
                _data[uid]!!.point.add(-rankConfig.decayPoint)
                _decayUsers[uid] = rankConfig.decayPoint
                userPoint[uid] = _data[uid]!!.point.value
            }
        }
        if (userPoint.isEmpty()) {
            return
        }
        _pvpDataAccess.decayUserRank(_pvpSeasonManager.currentSeasonNumber, userPoint)
        // trừ điểm của những user đang online, decayPoint đang là số dương
        _decayUsers.forEach { (uid, decayPoint) ->
            val controller = _usersManager.getUserController(uid) as? LegacyUserController
            controller?.updatePvpRanking(-decayPoint, 0, 0)
        }
    }

    override fun getDecayPointUser(uid: Int): Int {
        if (::_decayUsers.isInitialized && _decayUsers.containsKey(uid)) {
            val result = _decayUsers[uid]!!
            // remove để chỉ hiện dialog decay 1 lần trong ngày
            _decayUsers.remove(uid)
            return result
        }
        return 0
    }

    override fun clearDataForOneUser(uid: Int) {
        _data.remove(uid)
    }

    private fun invokeReturnValue(action: () -> Any?, maxRetry: Int): Any? {
        for (i in 0 until maxRetry) {
            try {
                return action()
            } catch (e: Exception) {
                _logger.log("[PvpRankingManager:invokeReturnValue:$i] message: ${e.message}")
            }
        }
        throw Exception("Could not invokeReturnValue $action")
    }
}