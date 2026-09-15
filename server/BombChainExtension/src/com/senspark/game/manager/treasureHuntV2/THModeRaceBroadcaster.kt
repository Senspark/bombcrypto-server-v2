package com.senspark.game.manager.treasureHuntV2

import com.senspark.common.cache.IMessengerService
import com.senspark.common.service.IScheduler
import com.senspark.common.utils.ILogger
import com.senspark.game.constant.ChannelKeys
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.declare.EnumConstants
import com.senspark.game.utils.serialize
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

/**
 * Đẩy trạng thái race TH mode sang th-mode-server qua Redis Pub/Sub.
 *
 * [record] chạy trên thread game mỗi lần phá block: chỉ ghi đè bản mới nhất của hero, không đụng Redis.
 * [flush] chạy mỗi giây trên scheduler, gom thành một PUBLISH (JSON array). th-mode-server chỉ giữ
 * giá trị mới nhất của mỗi hero nên gộp không làm mất gì.
 */
class THModeRaceBroadcaster(
    private val _messengerService: IMessengerService,
    private val _scheduler: IScheduler,
    private val _logger: ILogger,
) {
    companion object {
        private const val FLUSH_TASK = "THModeRaceBroadcaster"
        private const val FLUSH_INTERVAL_MS = 1000
    }

    private val _latest = ConcurrentHashMap<String, DataThModeRedis>()

    fun start() {
        _scheduler.schedule(FLUSH_TASK, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, ::flush)
    }

    fun record(raceId: Int, uid: Int, userName: String, hero: Hero, ticket: Int, poolIndex: Int) {
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

            _latest["$raceId:$network:${hero.heroId}"] = DataThModeRedis(
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
        } catch (e: Exception) {
            // ignore
        }
    }

    /**
     * Chạy cùng thread scheduler với calculateReward() (chỗ đổi race), nên chỉ cần sắp batch theo raceId
     * là entry race cũ luôn đi trước race mới.
     * Phải bắt mọi exception: scheduleAtFixedRate huỷ task vĩnh viễn nếu action ném lỗi.
     */
    fun flush() {
        try {
            val batch = mutableListOf<DataThModeRedis>()
            for (key in _latest.keys) {
                // remove() trả về bản mới nhất; ghi đến sau khi key đã bị remove sẽ vào batch kế tiếp
                _latest.remove(key)?.let { batch.add(it) }
            }
            if (batch.isEmpty()) {
                return
            }
            batch.sortBy { it.raceId }
            _messengerService.publish(ChannelKeys.SV_TH_MODE_RACE_CHANNEL, batch.serialize())
        } catch (e: Exception) {
            _logger.error("[THModeRaceBroadcaster] flush error: ${e.message}")
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
