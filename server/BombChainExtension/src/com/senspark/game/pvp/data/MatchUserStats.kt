package com.senspark.game.pvp.data

import com.senspark.common.pvp.IMatchUserStats
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MatchUserStats(
    @SerialName("client_type") override val clientType: String,
    @SerialName("session_type") override val sessionType: String,
    @SerialName("ip") override val ip: String,
    @SerialName("country") override val country: String,
    @SerialName("country_iso_code") override val countryIsoCode: String,
    @SerialName("is_udp_enabled") override val isUdpEnabled: Boolean,
    @SerialName("is_encrypted") override val isEncrypted: Boolean,
    @SerialName("latency") override val latency: Int,
    @SerialName("time_delta") override val timeDelta: Int,
    @SerialName("loss_rate") override val lossRate: Float,
) : IMatchUserStats