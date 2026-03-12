package com.senspark.game.pvp.utility

import com.senspark.common.pvp.*
import com.senspark.game.api.*
import com.senspark.game.api.redis.*
import com.senspark.game.pvp.data.*
import com.senspark.game.pvp.delta.*
import com.senspark.game.pvp.info.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

object JsonUtility {
    @OptIn(ExperimentalSerializationApi::class)
    val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            polymorphic(IFallingBlockInfo::class) {
                subclass(FallingBlockInfo::class)
            }
            polymorphic(IBlockInfo::class) {
                subclass(BlockInfo::class)
            }
            polymorphic(IBlockStateDelta::class) {
                subclass(BlockStateDelta::class)
            }
            polymorphic(IBombStateDelta::class) {
                subclass(BombStateDelta::class)
            }
            polymorphic(IFallingBlockData::class) {
                subclass(FallingBlockData::class)
            }
            polymorphic(IHeroStateDelta::class) {
                subclass(HeroStateDelta::class)
            }
            polymorphic(IMapInfo::class) {
                subclass(MapInfo::class)
            }
            polymorphic(IMatchData::class) {
                subclass(MatchData::class)
            }
            polymorphic(IMatchFinishData::class) {
                subclass(MatchFinishData::class)
            }
            polymorphic(IMatchHeroInfo::class) {
                subclass(MatchHeroInfo::class)
                defaultDeserializer { MatchHeroInfo.serializer() }
            }
            polymorphic(IMatchHistoryInfo::class) {
                subclass(MatchHistoryInfo::class)
            }
            polymorphic(IMatchInfo::class) {
                subclass(MatchInfo::class)
                defaultDeserializer { MatchInfo.serializer() }
            }
            polymorphic(IMatchObserveData::class) {
                subclass(MatchObserveData::class)
            }
            polymorphic(IMatchReadyData::class) {
                subclass(MatchReadyData::class)
            }
            polymorphic(IMatchResultHeroInfo::class) {
                subclass(MatchResultHeroInfo::class)
            }
            polymorphic(IMatchResultInfo::class) {
                subclass(MatchResultInfo::class)
            }
            polymorphic(IMatchResultUserInfo::class) {
                subclass(MatchResultUserInfo::class)
            }
            polymorphic(IMatchRuleInfo::class) {
                subclass(MatchRuleInfo::class)
                defaultDeserializer { MatchRuleInfo.serializer() }
            }
            polymorphic(IMatchStartData::class) {
                subclass(MatchStartData::class)
            }
            polymorphic(IMatchStats::class) {
                subclass(MatchStats::class)
            }
            polymorphic(IMatchTeamInfo::class) {
                subclass(MatchTeamInfo::class)
                defaultDeserializer { MatchTeamInfo.serializer() }
            }
            polymorphic(IMatchUserInfo::class) {
                subclass(MatchUserInfo::class)
                defaultDeserializer { MatchUserInfo.serializer() }
            }
            polymorphic(IMatchUserStats::class) {
                subclass(MatchUserStats::class)
            }
            polymorphic(IPingPongData::class) {
                subclass(PingPongData::class)
            }
            polymorphic(IPvpFixtureMatchInfo::class) {
                subclass(PvpFixtureMatchInfo::class)
            }
            polymorphic(IPvpFixtureMatchUserInfo::class) {
                subclass(PvpFixtureMatchUserInfo::class)
            }
            polymorphic(IPvpJoinQueueInfo::class) {
                subclass(PvpJoinQueueInfo::class)
            }
            polymorphic(IPvpResultInfo::class) {
                subclass(PvpResultInfo::class)
            }
            polymorphic(IPvpResultUserInfo::class) {
                subclass(PvpResultUserInfo::class)
            }
            polymorphic(IUseEmojiData::class) {
                subclass(UseEmojiData::class)
            }

        }
    }
}