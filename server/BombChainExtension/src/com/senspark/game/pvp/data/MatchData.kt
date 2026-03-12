package com.senspark.game.pvp.data

import com.senspark.common.pvp.IMatchData
import com.senspark.common.pvp.IMatchResultInfo
import com.senspark.common.pvp.MatchStatus
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private class MatchStatusSerializer : KSerializer<MatchStatus> {
    override val descriptor = PrimitiveSerialDescriptor("match_status", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: MatchStatus) {
        encoder.encodeInt(value.ordinal)
    }

    override fun deserialize(decoder: Decoder): MatchStatus {
        return MatchStatus.values()[decoder.decodeInt()]
    }
}

@Serializable
class MatchData(
    @SerialName("id") override val id: String,
    @Serializable(with = MatchStatusSerializer::class)
    @SerialName("status") override var status: MatchStatus,
    @SerialName("observer_count") override var observerCount: Int,
    @SerialName("start_timestamp") override var startTimestamp: Long,
    @SerialName("ready_start_timestamp") override var readyStartTimestamp: Long,
    @SerialName("round_start_timestamp") override var roundStartTimestamp: Long,
    @SerialName("round") override var round: Int,
    @SerialName("results") override val results: MutableList<IMatchResultInfo>,
) : IMatchData