package com.senspark.game.extension.modules

import com.senspark.common.service.IGlobalService
import com.senspark.common.service.IServerService
import com.senspark.game.extension.ServerServices
import kotlin.reflect.KClass

interface ISvServicesContainer : IGlobalService {
    fun get(serverType: ServerType): ServerServices
    fun forEach(block: (ServerServices) -> Unit)
    fun <T : IServerService> filter(clazz: KClass<T>): List<T>
}

