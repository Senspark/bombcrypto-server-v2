package com.senspark.game.pvp.info

import com.senspark.game.pvp.entity.BlockType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private class BlockTypeSerializer : KSerializer<BlockType> {
    override val descriptor = PrimitiveSerialDescriptor("block_type", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: BlockType) {
        encoder.encodeInt(value.ordinal)
    }

    override fun deserialize(decoder: Decoder): BlockType {
        return BlockType.values()[decoder.decodeInt()]
    }
}

@Serializable
class BlockInfo(
    @Serializable(with = BlockTypeSerializer::class)
    @SerialName("block_type")
    override val type: BlockType,
    @SerialName("x") override val x: Int,
    @SerialName("y") override val y: Int,
    @SerialName("health") override val health: Int,
) : IBlockInfo