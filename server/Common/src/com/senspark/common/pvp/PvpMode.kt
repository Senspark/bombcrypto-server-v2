package com.senspark.common.pvp

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object PvpModeSerializer : KSerializer<PvpMode> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PvpMode", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: PvpMode) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): PvpMode {
        return PvpMode.fromValue(decoder.decodeInt())
    }
}

@Serializable(with = PvpModeSerializer::class)
enum class PvpMode(val value: Int) {
    FFA_2(1 shl 0), // Team size=1, room size=2, default (i.e. 1v1).
    FFA_3(1 shl 1), // Team size=1, room size=3 (i.e. 1v1v1).
    FFA_4(1 shl 2), // Team size=1, room size=4 (i.e. 1v1v1v1).
    Team_2v2(1 shl 3), // Team size=2, room size=4 (i.e. 2v2).
    FFA_2_B3(1 shl 4), // Team size=1, room size=2, round=3, non-drawable (i.e. 1v1, bo3, non-drawable).
    FFA_2_B5(1 shl 5), // Team size=1, room size=2, round=5, non-drawable (i.e. 1v1, bo5, non-drawable).
    FFA_2_B7(1 shl 6), // Team size=1, room size=2, round=7, non-drawable (i.e. 1v1, bo7, non-drawable).
    Team_3v3(1 shl 7), // Team size=3, room size=6 (i.e. 3v3).
    BATTLE_ROYALE(1 shl 8); // Team size=1, room size=6 (i.e. 6 players FFA).

    companion object {
        private val types = PvpMode.values().associateBy { it.value }
        fun fromValue(value: Int) = types[value] ?: throw Exception("Could not find type: $value")
    }
}