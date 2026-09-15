package com.senspark.common.cache

import com.senspark.common.service.IGlobalService
import com.senspark.common.service.IService

// Low-latency [IMessengerService] for a few hot streams (e.g. AP_MAP_EXPLODE_RESULT_STR).
interface IFastStreamRedis : IService, IGlobalService {
    fun send(key: String, message: String)

    /**
     * Callback mà return true thì sẽ tự động xoá message
     */
    fun listen(key: String, callback: (Message) -> Boolean)
    fun delete(key: String, id: String)
}
