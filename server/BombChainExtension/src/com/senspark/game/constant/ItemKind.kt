package com.senspark.game.constant

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ItemKindSerializer : KSerializer<ItemKind> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("type", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ItemKind) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): ItemKind {
        return ItemKind.valueOf(decoder.decodeString())
    }
}

@Serializable(with = ItemKindSerializer::class)
enum class ItemKind {
    MVP,
    NORMAL,
    PREMIUM,
}