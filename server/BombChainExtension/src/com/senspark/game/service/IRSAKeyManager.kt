package com.senspark.game.service

import com.senspark.common.service.IScheduler
import com.senspark.common.service.IService
import com.senspark.common.service.Service
import com.senspark.common.utils.ILogger
import com.senspark.game.utils.RSA
import com.senspark.game.utils.RsaKeys
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Quản lý RSA key, cache keys trong 10 phút & tự clear key
 */
@Service("IRSAKeyManager")
interface IRSAKeyManager : IService {
    fun getKey(uid: Int): RsaKeys
    fun removeKey(uid: Int)
}

class RSAKeyManager(
//    private val _cacheService: ICacheService,
    private val _logger: ILogger,
    private val _scheduler: IScheduler
) : IRSAKeyManager {

    init {
        _scheduler.schedule(TAG, DELAY_TIME, INTERVAL_TIME) {
            autoExpireKeys()
        }
    }

    companion object {
        private const val TAG = "IRSAKeyManager"
        private val DELAY_TIME: Int = 5.minutes.inWholeMilliseconds.toInt()
        private val INTERVAL_TIME: Int = 5.minutes.inWholeMilliseconds.toInt()
        private val EXPIRED_TIME: Long = 1.hours.inWholeSeconds
    }

    private val _keys: ConcurrentHashMap<Int, Pair<RsaKeys, Instant>> = ConcurrentHashMap(mutableMapOf())

    override fun getKey(uid: Int): RsaKeys {
        if (_keys.containsKey(uid)) {
            val data = _keys[uid]!!
            if (data.second.isAfter(Instant.now())) {
                _logger.log("Get cached RSA keys for user $uid")
                return data.first
            }
        }
        _logger.log("Generate new RSA keys for user $uid")
        val rsaKeys = RSA.generateRSAKeys()
        _keys[uid] = Pair(rsaKeys, Instant.now().plusSeconds(EXPIRED_TIME))
//        val cacheService = mainGameExtension.serviceLocator.resolve<ICacheService>()
//        cacheService.setToHash(CachedKeys.PRIVATE_KEY, uid.toString(), rsaKeys.privateKeyStr)
        return rsaKeys
    }

    override fun removeKey(uid: Int) {
        _keys.remove(uid)
        _logger.log("Remove RSA keys for user $uid")
    }

    override fun destroy() {
        _keys.clear()
        _scheduler.clear(TAG)
    }

    private fun autoExpireKeys() {
        val now = Instant.now()
        _keys.forEach { (uid, data) ->
            if (data.second.isBefore(now)) {
                _keys.remove(uid)
                _logger.log("Expired RSA keys for user $uid")
            }
        }
    }
}