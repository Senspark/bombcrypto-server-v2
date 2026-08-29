package com.senspark.game.api

import com.senspark.common.pvp.IMatchUserInfo
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.IEnvManager
import com.senspark.game.pvp.utility.JsonUtility
import com.senspark.game.utils.serialize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

@Serializable
class PvpJoinQueueInfo(
    @SerialName("username") override val username: String,
    @SerialName("pings") override val pings: Map<String, Int>,
    @SerialName("data") override val info: IMatchUserInfo,
    @SerialName("game_mode") override val gameMode: Int = 1,
    @SerialName("wager_mode") override val wagerMode: Int = 0,
    @SerialName("wager_tier") override val wagerTier: Int = 0,
    @SerialName("wager_token") override val wagerToken: Int = 0,
    @SerialName("network") override val network: String = "",
) : IPvpJoinQueueInfo