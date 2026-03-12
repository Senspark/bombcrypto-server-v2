package com.senspark.common.pvp

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = PvpFixtureMatchStatus::class)
object PvpFixtureMatchStatusSerializer : KSerializer<PvpFixtureMatchStatus> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PvpFixtureMatchStatus", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: PvpFixtureMatchStatus) {
        encoder.encodeInt(value.ordinal)
    }

    override fun deserialize(decoder: Decoder): PvpFixtureMatchStatus {
        return PvpFixtureMatchStatus.values()[decoder.decodeInt()]
    }
}

@Serializable(with = PvpFixtureMatchStatusSerializer::class)
enum class PvpFixtureMatchStatus {
    Pending,
    Aborted,
    Completed,
}

interface IPvpFixtureMatchUserInfo {
    /** Score result. */
    val score: Int

    val userId: Int
    val username: String
    val displayName: String
    val rank: Int
}

interface IPvpFixtureMatchInfo {
    val id: String
    val status: PvpFixtureMatchStatus

    /** Matchmaking period. */
    val findBeginTimestamp: Long
    val findEndTimestamp: Long
    val finishTimestamp: Long

    val mode: PvpMode
    val info: List<IPvpFixtureMatchUserInfo>
}