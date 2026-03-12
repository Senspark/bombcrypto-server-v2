package com.senspark.game.service

import com.senspark.common.service.IService
import com.senspark.common.service.IServiceLocator

class ServiceLocator : IServiceLocator {
    private val _services = mutableMapOf<String, IService>()
    private val _nameCache = ServiceNameCache()
    override fun provide(service: IService) {
        val type = service.javaClass
        val name = _nameCache.getServiceName(type)
        val currentService = _services[name]
        currentService?.destroy()
        _services[name] = service
    }

    override fun resolve(type: Class<*>): IService {
        val name = _nameCache.getServiceName(type)
        return _services[name] ?: throw Exception("Cannot find the requested service: $name")
    }
}