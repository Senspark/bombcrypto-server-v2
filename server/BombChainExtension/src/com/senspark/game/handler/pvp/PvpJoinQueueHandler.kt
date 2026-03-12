package com.senspark.game.handler.pvp

import com.senspark.common.pvp.PvpMode
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.user.ITrGameplayManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json


@Serializable
class NetworkInfo(
    @SerialName("zone_id") val zoneId: String,
    @SerialName("ping") val ping: Int,
)

@Serializable
class JoinPvpQueueRequest(
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("mode") val mode: Int = PvpMode.FFA_2.value,
    @SerialName("match_id") val matchId: String? = null, // Old client may be null.
    @SerialName("test") val test: Boolean,
    @SerialName("hero_id") val heroId: Int,
    @SerialName("boosters") val boosters: List<Int>,
    @SerialName("pings") private val _pings: List<NetworkInfo>,
    @SerialName("avatar") val avatar: Int? = null,
) {
    @Transient
    val pings = _pings.associate { it.zoneId to it.ping }

    companion object {
        private val jsonConfig = Json { 
            ignoreUnknownKeys = true 
        }
        
        fun parse(data: ISFSObject): JoinPvpQueueRequest {
            val json = data.toJson()
            return jsonConfig.decodeFromString(json)
        }
    }
}

class PvpJoinQueueHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.JOIN_PVP_QUEUE_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val response = SFSObject()
        try {
            val trGameplayManager = services.get<ITrGameplayManager>()
            if(trGameplayManager.isPlaying(controller.userId))
                throw CustomException("Your account is already playing on other network")

            val request = JoinPvpQueueRequest.parse(data)
            controller.joinPvpQueue(
                request.mode,
                request.matchId ?: "",
                request.test,
                request.heroId,
                request.boosters,
                request.pings,
                request.avatar?: 0, //Support client cũ không gửi avatar
            )

            trGameplayManager.joinPvp(controller.userId, controller.dataType)
            return sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            controller.logger.error(serverCommand, ex)
            return sendError(controller, requestId, 100, ex.message)
        }
    }
}