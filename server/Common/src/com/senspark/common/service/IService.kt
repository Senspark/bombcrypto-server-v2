package com.senspark.common.service

interface IService {
    fun destroy()
}

/**
 * Service sử dụng chung cho tất cả các loại Server
 */
interface IGlobalService {
    fun initialize()
}

/**
 * Service dành riêng cho 1 loại Server
 */
interface IServerService {
    fun initialize()
}