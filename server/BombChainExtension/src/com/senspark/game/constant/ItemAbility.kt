package com.senspark.game.constant

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ItemAbilitySerializer : KSerializer<ItemAbility> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("type", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ItemAbility) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): ItemAbility {
        val intValue = decoder.decodeInt()
        return ItemAbility.fromValue(intValue)
    }
}

@Serializable(with = ItemAbilitySerializer::class)
enum class ItemAbility(val value: Int) {
    DMG_PLUS_1(1),
    BOMB_PLUS_1(2),
    SPEED_PLUS_1(3),
    RANGE_PLUS_1(4),
    HP_PLUS_1(5);

    companion object {
        private val types = ItemAbility.values().associateBy { it.value }
        fun fromValue(value: Int): ItemAbility = types[value] ?: throw Exception("Could not find type: $value")
    }
}