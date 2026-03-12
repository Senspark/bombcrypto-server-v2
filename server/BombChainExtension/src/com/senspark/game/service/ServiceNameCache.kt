package com.senspark.game.service

import com.senspark.common.service.Service

class ServiceNameCache {
    private val _serviceNames = mutableMapOf<Class<*>, String>()

    fun getServiceName(type: Class<*>): String {
        val result = _serviceNames[type]
        if (result != null) {
            return result
        }
        val interfaces = type.interfaces.toMutableList()
        if (type.isInterface) {
            interfaces.add(type)
        }
        for (it in interfaces) {
            val annotation = it.getAnnotation(Service::class.java)
            if (annotation is Service) {
                val name = annotation.name
                _serviceNames[type] = name
                return name
            }
        }
        throw Exception("The requested service is not registered: ${type.name}")
    }
}