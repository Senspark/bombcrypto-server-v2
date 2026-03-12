package com.senspark.game.constant

import com.senspark.game.db.IUserDataAccess
import com.senspark.game.inventory.*
import com.senspark.game.manager.hero.IUserHeroTRManager
import com.senspark.game.manager.material.IUserMaterialManager
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

object ProductTypeSerializer : KSerializer<ItemType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("productType", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ItemType) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): ItemType {
        // Admittedly, this would accept "Error" in addition to "error".
        return ItemType.valueOf(decoder.decodeString().uppercase(Locale.getDefault()))
    }
}


@Serializable(with = ProductTypeSerializer::class)
enum class ItemType(val value: Int, val maxActive: Int) {
    BOMB(1, 1),
    TRAIL(2, 1),
    WING(3, 1),
    HERO(4, 0),
    BOOSTER(5, 0),
    MISC(6, 0),
    REWARD(7, 0),
    FIRE(8, 1),
    MYSTERY_BOX(9, 0),
    MATERIAL(10, 0),
    EMOJI(11, 4),
    AVATAR(12, 1);

    fun newInstance(
        heroTRManager: IUserHeroTRManager,
        userMaterialManager: IUserMaterialManager,
        userDataAccess: IUserDataAccess
    ): IUserInventory {
        return when (this) {
            BOMB, TRAIL, WING, FIRE, EMOJI, AVATAR -> UserSkin(userDataAccess)
            HERO -> UserHeroesInventory(heroTRManager)
            BOOSTER, MISC -> UserBooster(userDataAccess)
            MATERIAL -> UserMaterialInventory(userMaterialManager)
            else -> throw Exception("Type not found ${this.name}")
        }
    }

    companion object {
        private val types = entries.associateBy { it.value }
        fun fromValue(value: Int) = types[value] ?: throw Exception("Could not find type: $value")
        fun hasValue(value: Int) = types.containsKey(value)
    }
}