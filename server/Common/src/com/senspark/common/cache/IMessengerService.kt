package com.senspark.common.cache

import com.senspark.common.service.IGlobalService
import com.senspark.common.service.IService

interface IMessengerService: IService, IGlobalService {
    /**
     * @param maxLen khác null thì XADD kèm `MAXLEN ~ maxLen` để stream không phình vô hạn
     */
    fun send(key: String, message: String, maxLen: Long? = null)

    /**
     * Redis Pub/Sub, fire-and-forget: không lưu vào dataset, không có lịch sử
     */
    fun publish(channel: String, message: String)

    /**
     * Callback mà return true thì sẽ tự động xoá message
     */
    fun listen(key: String, callback: (Message) -> Boolean)
    fun delete(key: String, id: String)
}

data class Message(val id: String, val key: String, val value: String)