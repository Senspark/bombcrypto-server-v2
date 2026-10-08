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
     * Pub/Sub bus: một channel chở nhiều loại message, phân biệt bằng `type`
     */
    fun publishBus(channel: String, type: String, data: String)

    /**
     * Callback chạy trên một thread riêng, tuần tự theo thứ tự message đến
     */
    fun onBus(channel: String, type: String, callback: (String) -> Unit)

    /**
     * Callback mà return true thì sẽ tự động xoá message
     */
    fun listen(key: String, callback: (Message) -> Boolean)
    fun delete(key: String, id: String)
}

data class Message(val id: String, val key: String, val value: String)