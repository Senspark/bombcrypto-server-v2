package com.senspark.game.pvp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PingPongData(
    @SerialName("request_id") override val requestId: Int,
    @SerialName("latencies") override val latencies: List<Int>,
    @SerialName("time_delta") override val timeDelta: List<Int>,
    @SerialName("loss_rates") override val lossRates: List<Float>,
) : IPingPongData