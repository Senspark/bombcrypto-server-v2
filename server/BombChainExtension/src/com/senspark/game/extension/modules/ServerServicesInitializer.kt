package com.senspark.game.extension.modules

import com.senspark.common.utils.IGlobalLogger
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.extension.GlobalServices
import com.senspark.game.extension.ServerServices
import com.senspark.game.manager.IEnvManager
import com.smartfoxserver.v2.extensions.SFSExtension

object ServerServicesInitializer {
    fun createServices(
        globalServices: GlobalServices,
        extension: SFSExtension?,
    ): ISvServicesContainer {
        val initializers = mutableListOf<IPerServerServicesInitializer>()

        val env = globalServices.get<IEnvManager>()
        val logger = globalServices.get<IGlobalLogger>()

        if (env.isBnbServer) {
            initializers.add(ServicesInitializerBnbPol(globalServices, extension))
        }
        if (env.isTonServer) {
            initializers.add(ServicesInitializerTon(globalServices))
        }
        if (env.isSolServer) {
            initializers.add(ServicesInitializerSol(globalServices))
        }
        if (env.isRonServer) {
            initializers.add(ServicesInitializerRon(globalServices, extension))
        }
        if (env.isBasServer) {
            initializers.add(ServicesInitializerBas(globalServices, extension))
        }
        if (env.isVicServer) {
            initializers.add(ServicesInitializerVic(globalServices, extension))
        }

        val services = initializers.map { e ->
            Pair(e.serverType, e.createService())
        }
        return SvServicesContainerContainer(services.toMap(), logger)
    }
}

interface IPerServerServicesInitializer {
    val serverType: ServerType
    fun createService(): ServerServices
}

internal typealias ServicesMap = Map<ServerType, ServerServices>

/**
 * Có 6 loại Server: BNB, TON, SOL, RON, BAS, VIC
 * Mỗi server phân biệt logic riêng cho từng loại server.
 */
enum class ServerType {
    BNB_POL,
    TON,
    SOL,
    RON,
    BAS,
    VIC
    ;

    companion object {
        fun from(d: DataType): ServerType {
            val n = when (d) {
                DataType.TON -> TON
                DataType.SOL -> SOL
                DataType.RON -> RON
                DataType.VIC -> VIC
                DataType.BAS -> BAS
                else -> BNB_POL
            }
            return n
        }
    }
}
