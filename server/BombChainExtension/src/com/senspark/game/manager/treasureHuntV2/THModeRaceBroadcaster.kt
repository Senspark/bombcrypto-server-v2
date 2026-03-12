package com.senspark.game.manager.treasureHuntV2

import com.senspark.common.cache.IMessengerService
import com.senspark.game.constant.StreamKeys
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.declare.EnumConstants
import com.senspark.game.utils.serialize
import kotlinx.serialization.Serializable

class THModeRaceBroadcaster(
    private val _messengerService: IMessengerService,
) {
    fun sendUserToRedis(raceId: Int, uid: Int, userName: String, hero: Hero, ticket: Int, poolIndex: Int) {
        try {
            val heroType: Int = if (hero.isHeroS) {
                2
            } else if (hero.isFakeS) {
                1
            } else {
                0
            }
            val network = when (hero.details.dataType) {
                EnumConstants.DataType.BSC -> 0
                EnumConstants.DataType.POLYGON -> 1
                else -> throw Exception("Invalid network type")
            }
            
            val result = DataThModeRedis(
                raceId = raceId,
                uid = uid,
                userName = userName,
                heroId = hero.heroId,
                stakeBcoin = hero.stakeBcoin,
                stakeSen = hero.stakeSen,
                ticketCount = ticket,
                poolIndex = poolIndex,
                network = network,
                heroType = heroType
            )
            _messengerService.send(StreamKeys.SV_TH_MODE_RACE, result.serialize())
        } catch (e: Exception) {
            // ignore
        }
    }
    
    @Serializable
    data class DataThModeRedis(
        val raceId: Int,
        val uid: Int,
        val userName: String,
        val heroId: Int,
        val stakeBcoin: Double,
        val stakeSen: Double,
        val ticketCount: Int,
        val poolIndex: Int,
        val network: Int,
        val heroType: Int
    )
}