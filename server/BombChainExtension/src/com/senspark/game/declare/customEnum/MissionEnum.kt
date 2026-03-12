package com.senspark.game.declare.customEnum

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

enum class MissionAction {
    WATCH_ADS, //done
    WIN_PVP, //done
    WIN_ADVENTURE, //done
    BUY_ITEM_P2P,
    BUY_HERO_P2P,
    GRIND_HERO, //done
    UPGRADE_HERO,//done
    USE_SHIELD, //done
    USE_KEY, //done
    WIN_STREAKS_PVP,
    PVP_RANK_TARGET,
    COMPLETE_ADVENTURE,
}

enum class MissionType(val isDailyMission: Boolean) {
    DAILY_GIFT(true),
    DAILY_QUEST(true),
    ACHIEVEMENT(false);
}

@Serializable(with = MissionRewardTypeSerializer::class)
enum class MissionRewardType {
    RANDOM_ALL,
    RANDOM_IN_LIST,
    TAKE_ALL_IN_LIST
}

object MissionRewardTypeSerializer : KSerializer<MissionRewardType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("type", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: MissionRewardType) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): MissionRewardType {
        // Admittedly, this would accept "Error" in addition to "error".
        return MissionRewardType.valueOf(decoder.decodeString().uppercase(Locale.getDefault()))
    }
}

@Serializable(with = MissionItemRewardTypeSerializer::class)
enum class MissionItemRewardType {
    BOOSTER,
    BOMB,
    WING,
    REWARD;
}

object MissionItemRewardTypeSerializer : KSerializer<MissionItemRewardType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("rewardType", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: MissionItemRewardType) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(input: Decoder): MissionItemRewardType {
        // Admittedly, this would accept "Error" in addition to "error".
        return MissionItemRewardType.valueOf(input.decodeString().uppercase(Locale.getDefault()))
    }
}

