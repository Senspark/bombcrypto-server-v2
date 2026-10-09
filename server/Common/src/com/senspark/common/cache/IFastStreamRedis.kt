package com.senspark.common.cache

import com.senspark.common.service.IGlobalService
import com.senspark.common.service.IService

// Low-latency Redis Pub/Sub for a few hot channels (e.g. AP_MAP_TREASURE_EVENT_CHANNEL).
interface IFastStreamRedis : IService, IGlobalService {
    fun send(key: String, message: String)

    /**
     * Callback chạy trên một thread riêng, tuần tự theo thứ tự message đến
     */
    fun listen(key: String, callback: (String) -> Unit)
}
