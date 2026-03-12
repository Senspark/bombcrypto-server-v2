package com.senspark.common.cache

import com.senspark.common.service.IGlobalService
import com.senspark.common.service.IService

interface IMessengerService: IService, IGlobalService {
    fun send(key: String, message: String)

    /**
     * Callback mà return true thì sẽ tự động xoá message
     */
    fun listen(key: String, callback: (Message) -> Boolean)
    fun delete(key: String, id: String)
}

data class Message(val id: String, val key: String, val value: String)