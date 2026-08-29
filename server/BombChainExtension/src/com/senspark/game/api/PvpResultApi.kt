package com.senspark.game.api

import com.senspark.common.pvp.IMatchRuleInfo
import com.senspark.common.pvp.IMatchRuleInfoClient
import com.senspark.common.pvp.IMatchTeamInfo
import com.senspark.common.pvp.PvpMode
import com.senspark.common.utils.ILogger
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.IPvpEnvManager
import com.senspark.game.pvp.info.MatchRuleInfo
import com.senspark.game.pvp.info.MatchRuleInfoClient
import com.senspark.game.pvp.info.MatchTeamInfo
import com.smartfoxserver.v2.entities.data.ISFSObject
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

@OptIn(ExperimentalSerializationApi::class)
private val _json = Json {
    ignoreUnknownKeys = true
    serializersModule = SerializersModule {
        polymorphic(IPvpResultInfoClient::class) {
            subclass(PvpResultInfoClient::class)
            defaultDeserializer { PvpResultInfoClient.serializer() }
        }
        polymorphic(IPvpResultUserInfoClient::class) {
            subclass(PvpResultUserInfoClient::class)
            defaultDeserializer { PvpResultUserInfoClient.serializer() }
        }
        polymorphic(IMatchRuleInfoClient::class) {
            subclass(MatchRuleInfoClient::class)
            defaultDeserializer { MatchRuleInfoClient.serializer() }
        }
        polymorphic(IPvpResultInfo::class) {
            subclass(PvpResultInfo::class)
            defaultDeserializer { PvpResultInfo.serializer() }
        }
        polymorphic(IPvpResultUserInfo::class) {
            subclass(PvpResultUserInfo::class)
            defaultDeserializer { PvpResultUserInfo.serializer() }
        }
        polymorphic(IMatchRuleInfo::class) {
            subclass(MatchRuleInfo::class)
            defaultDeserializer { MatchRuleInfo.serializer() }
        }
        polymorphic(IMatchTeamInfo::class) {
            subclass(MatchTeamInfo::class)
            defaultDeserializer { MatchTeamInfo.serializer() }
        }
    }
}

@Serializable
class PvpResultUserInfoClient(
    override var server_id: String,
    override var is_bot: Boolean,
    override var team_id: Int,
    override var user_id: Int,
    override val username: String,
    override val rank: Int,
    override val point: Int,
    override var match_count: Int,
    override var win_match_count: Int,
    override var delta_point: Int,
    override var used_boosters: Map<Int, Int>,
    override val quit: Boolean,
    override val heroId: Int,
    override val damageSource: Int,
    override val rewards: Map<Int, Float>,
    override val collectedItems: List<Int>,
    override val ranking: Int,
) : IPvpResultUserInfoClient

@Serializable
class PvpResultInfoClient(
    override val id: String,
    override val serverId: String,
    override val timestamp: Long,
    override val mode: PvpMode,
    override var is_draw: Boolean,
    override var winning_team: Int,
    override val scores: List<Int>,
    override val duration: Int,
    override val rule: IMatchRuleInfoClient,
    override val team: List<IMatchTeamInfo>,
    override val info: List<IPvpResultUserInfoClient>,
    override val wagerMode: Int = 0,
    override val wagerTier: Int = 0,
    override val wagerToken: Int = 0,
    override var signature: String? = null,
    override var integrityLogs: String? = null,
) : IPvpResultInfoClient {
    companion object {
        fun parse(data: IPvpResultInfoClient): String {
            return _json.encodeToString(data)
        }
    }
}

@Serializable
class PvpResultUserInfo(
    override val serverId: String,
    override val isBot: Boolean,
    override val teamId: Int,
    override val userId: Int,
    override val username: String,
    override val rank: Int,
    override val point: Int,
    override val matchCount: Int,
    override val winMatchCount: Int,
    override val deltaPoint: Int,
    override val usedBoosters: Map<Int, Int>,
    override val quit: Boolean,
    override val heroId: Int,
    override val damageSource: Int,
    override val rewards: Map<Int, Float>,
    override val collectedItems: List<Int>,
    override val ranking: Int,
) : IPvpResultUserInfo

@Serializable
class PvpResultInfo(
    override val id: String,
    override val serverId: String,
    override val timestamp: Long,
    override val mode: PvpMode,
    override val isDraw: Boolean,
    override val winningTeam: Int,
    override val scores: List<Int>,
    override val duration: Int,
    override val rule: IMatchRuleInfo,
    override val team: List<IMatchTeamInfo>,
    override val info: List<IPvpResultUserInfo>,
    override val wagerMode: Int = 0,
    override val wagerTier: Int = 0,
    override val wagerToken: Int = 0,
    override var signature: String? = null,
    override var integrityLogs: String? = null,
) : IPvpResultInfo {
    companion object {
        fun parse(data: ISFSObject): IPvpResultInfo {
            val json = data.toJson()
            return parse(json)
        }

        fun parse(data: String): IPvpResultInfo {
            return _json.decodeFromString<PvpResultInfo>(data)
        }
        
        fun parse(data: IPvpResultInfo): String {
            return _json.encodeToString(data)
        }
    }
}