package com.senspark.game.pvp.manager

import com.senspark.common.cache.ICacheService
import com.senspark.common.utils.ILogger
import com.senspark.game.api.*
import com.senspark.game.pvp.HandlerCommand
import com.senspark.game.pvp.data.*
import com.senspark.game.pvp.entity.UserPvpProperty
import com.senspark.game.pvp.info.MatchRuleInfoClient
import com.senspark.game.pvp.utility.JsonUtility
import com.senspark.game.utils.ISender
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.encodeToString
import javax.crypto.SecretKey

class DefaultMessageBridge(
    private val _logger: ILogger,
    private val _sender: ISender,
    private val _cache: ICacheService
) : IMessageBridge {
    private val _json = JsonUtility.json

    override fun ping(data: IPingPongData, users: List<User>) {
        val params = SFSObject.newFromJsonData(_json.encodeToString(data))
        val usersByUdp = users.groupBy { it.session.isUdpEnabled }
        usersByUdp.forEach { (isUdpEnabled, items) ->
            val listUsers = items.associateWith { it.getProperty(UserPvpProperty.AES_KEY) as SecretKey }
            _sender.sendWithEncrypt(
                HandlerCommand.PingPong,
                params,
                listUsers,
                isUdpEnabled,
            )
        }
    }

    override fun startReady(users: List<User>) {
        val listUsers = users.associateWith { it.getProperty(UserPvpProperty.AES_KEY) as SecretKey }
        _sender.sendWithEncrypt(
            HandlerCommand.StartReady,
            SFSObject(),
            listUsers,
            false,
        )
    }

    override fun ready(data: IMatchReadyData, users: List<User>) {
        val listUsers = users.associateWith { it.getProperty(UserPvpProperty.AES_KEY) as SecretKey }
        _sender.sendWithEncrypt(
            HandlerCommand.ObserverReady,
            SFSObject.newFromJsonData(_json.encodeToString(data)),
            listUsers,
            false,
        )
    }

    override fun finishReady(users: List<User>) {
        val listUsers = users.associateWith { it.getProperty(UserPvpProperty.AES_KEY) as SecretKey }
        _sender.sendWithEncrypt(
            HandlerCommand.FinishReady,
            SFSObject(),
            listUsers,
            false,
        )
    }

    override fun startRound(data: IMatchStartData, users: List<User>) {
        val listUsers = users.associateWith { it.getProperty(UserPvpProperty.AES_KEY) as SecretKey }
        _sender.sendWithEncrypt(
            HandlerCommand.StartRound,
            SFSObject.newFromJsonData(_json.encodeToString(data)),
            listUsers,
            false,
        )
    }

    override fun useEmoji(data: IUseEmojiData, users: List<User>) {
        val listUsers = users.associateWith { it.getProperty(UserPvpProperty.AES_KEY) as SecretKey }
        _sender.sendWithEncrypt(
            HandlerCommand.ObserverUseEmoji,
            SFSObject.newFromJsonData(_json.encodeToString(data)),
            listUsers,
            false,
        )
    }

    override fun bufferFallingBlocks(data: IFallingBlockData, users: List<User>) {
        val listUsers = users.associateWith { it.getProperty(UserPvpProperty.AES_KEY) as SecretKey }
        _sender.sendWithEncrypt(
            HandlerCommand.ObserverFallingBlock,
            SFSObject.newFromJsonData(_json.encodeToString(data)),
            listUsers,
            false,
        )
    }

    override fun changeState(data: IMatchObserveData, users: List<User>) {
        val params = SFSObject.newFromJsonData(_json.encodeToString(data))
        val sender: (Boolean, List<User>) -> Unit = { isUdpEnabled, items ->
            val listUsers = items.associateWith { it.getProperty(UserPvpProperty.AES_KEY) as SecretKey }
            _sender.sendWithEncrypt(
                HandlerCommand.ObserverChangeState,
                params,
                listUsers,
                isUdpEnabled,
            )
        }
        // Try to use udp if there are only hero position change events.
        val canUseUdp = data.bombDelta.isEmpty() &&
            data.blockDelta.isEmpty() &&
            data.heroDelta.all { it.base == null }
        if (canUseUdp) {
            val usersByUdp = users.groupBy { it.session.isUdpEnabled }
            usersByUdp.forEach(sender)
        } else {
            sender(false, users)
        }
    }

    override fun finishRound(data: IMatchFinishData, users: List<User>) {
        val listUsers = users.associateWith { it.getProperty(UserPvpProperty.AES_KEY) as SecretKey }
        _sender.sendWithEncrypt(
            HandlerCommand.FinishRound,
            SFSObject.newFromJsonData(_json.encodeToString(data)),
            listUsers,
            false,
        )
    }

    override fun finishMatch(data: IPvpResultInfo, users: List<User>) {
        //Tạm thời convert biến để client cũ dùng đc, sau này update sẽ bỏ bước này
        val dataClient = createResultInfoClient(data)
        val json = PvpResultInfoClient.parse(dataClient)

        val listUsers = users.associateWith { it.getProperty(UserPvpProperty.AES_KEY) as SecretKey }

        //Gửi về cho client kết thúc pvp
        _sender.sendWithEncrypt(
            HandlerCommand.FinishMatch,
            SFSObject.newFromJsonData(json),
            listUsers,
            false,
        )
    }

    private fun createResultInfoClient(resultInfo: IPvpResultInfo): IPvpResultInfoClient {
        val info = PvpResultInfoClient(
            id = resultInfo.id,
            serverId = resultInfo.serverId,
            timestamp = resultInfo.timestamp,
            mode = resultInfo.mode,
            is_draw = resultInfo.isDraw,
            winning_team = resultInfo.winningTeam,
            scores = resultInfo.scores,
            duration = resultInfo.duration,
            rule = MatchRuleInfoClient(
                room_size = resultInfo.rule.roomSize,
                team_size = resultInfo.rule.teamSize,
                round = resultInfo.rule.round,
                can_draw = resultInfo.rule.canDraw,
                is_tournament = resultInfo.rule.isTournament
            ),
            team = resultInfo.team,
            info = resultInfo.info.mapIndexed { index, userInfo ->
                PvpResultUserInfoClient(
                    server_id = userInfo.serverId,
                    is_bot = userInfo.isBot,
                    team_id = userInfo.teamId,
                    user_id = userInfo.userId,
                    username = userInfo.username,
                    rank = userInfo.rank,
                    point = userInfo.point,
                    match_count = userInfo.matchCount,
                    win_match_count = userInfo.winMatchCount,
                    delta_point = userInfo.deltaPoint,
                    used_boosters = userInfo.usedBoosters,
                    quit = userInfo.quit,
                    heroId = userInfo.heroId,
                    damageSource = userInfo.damageSource,
                    rewards = userInfo.rewards,
                    collectedItems = userInfo.collectedItems,
                )
            }
        )
        return info
    }
}