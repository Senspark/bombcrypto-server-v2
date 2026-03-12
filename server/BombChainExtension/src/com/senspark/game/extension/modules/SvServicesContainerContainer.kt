package com.senspark.game.extension.modules

import com.senspark.common.service.IServerService
import com.senspark.common.utils.ILogger
import com.senspark.game.extension.ServerServices
import kotlin.reflect.KClass
import kotlin.reflect.cast

class SvServicesContainerContainer(
    private val _services: ServicesMap,
    private val _logger: ILogger,
) : ISvServicesContainer {

    private var _initialized = false

    override fun initialize() {
        if (_initialized) {
            return
        }
        _initialized = true
        _services.forEach { (_, v) ->
            val names = mutableListOf<String>()
            v.forEach { s ->
                val name = s.javaClass.simpleName
                s.initialize()
                names.add(name)
            }
            _logger.log("Initialized container ${v.name} services: ${names.joinToString(", ")}")
        }
    }

    override fun get(serverType: ServerType): ServerServices {
        return _services[serverType] ?: throw Exception("NetServices not found for $serverType")
    }

    override fun forEach(block: (ServerServices) -> Unit) {
        _services.values.forEach(block)
    }

    override fun <T : IServerService> filter(clazz: KClass<T>): List<T> {
        val list = mutableListOf<T>()
        forEach { t ->
            t.forEach { s ->
                if (clazz.isInstance(s)) {
                    list.add(clazz.cast(s))
                }
            }
        }
        return list
    }
}