package com.senspark.game.extension

import com.senspark.common.extension.IApiExtension
import com.senspark.common.service.IScheduler
import com.senspark.common.utils.IGlobalLogger
import com.senspark.game.extension.events.*
import com.senspark.game.extension.helper.*
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IUsersManager
import com.senspark.game.utils.SignatureUtils
import com.senspark.lib.db.ILibDataAccess
import com.smartfoxserver.v2.core.SFSEventType
import com.smartfoxserver.v2.entities.Zone
import com.smartfoxserver.v2.extensions.SFSExtension
import java.time.Instant

class MainGameExtension : SFSExtension(), IBombGameServer, IZoneExtension, IApiExtension {

    companion object {
        private lateinit var services: GlobalServices
        private lateinit var timeInitServer: Instant
        val Services: GlobalServices
            get() {
                return services
            }

        val TimeInitServer get() = timeInitServer
    }

    override fun destroy() {
        super.destroy()
        services.get<IScheduler>().clearAll()
    }

    override fun init() {
        val extension = this

        println("====== Server start init ========")
        services = MainGameExtensionModules.initModules(extension)
        println("====== Init services done ========")

        val envManager = services.get<IEnvManager>()
        val logger = services.get<IGlobalLogger>()
        println("====== SERVER ID ${envManager.serverId} ========")

        initHandlers(parentZone, this, services)
        println("====== Init handlers done ========")

        SignatureUtils.initialize(envManager.messageSalt)
        initEvents()
        println("====== Server done init ========")
        logger.log("====== SERVER ID ${envManager.serverId} init done ========")

//        Runtime.getRuntime().addShutdownHook(Thread {
//            println("Shutting down... Disconnect all users")
//            this.clearAllHandlers()
//            services.get<ISvServicesContainer>().forEach { t ->
//                t.get<IUsersManager>().dispose()
//            }
//        })

        timeInitServer = Instant.now()
    }

    private fun initHandlers(
        zone: Zone,
        extension: SFSExtension,
        services: GlobalServices,
    ) {
        val envManager = services.get<IEnvManager>()
        val helper = AddRequestHandlerHelper { requestId, theClass ->
            addRequestHandler(requestId, theClass)
        }

        val handlers = mutableSetOf<IServerInitializer>()
        handlers.add(ServerInitializerAll(services, zone, extension))
        val airDropServerInitializer = ServerInitializerAirDrop()

        if (envManager.isTonServer) {
            handlers.add(ServerInitializerTon(services))
            handlers.add(airDropServerInitializer)
        }
        if (envManager.isSolServer) {
            handlers.add(ServerInitializerSol(services))
            handlers.add(airDropServerInitializer)
        }
        if (envManager.isRonServer) {
            handlers.add(ServerInitializerRon(services))
            handlers.add(airDropServerInitializer)
        }
        if (envManager.isVicServer) {
            handlers.add(ServerInitializerVic(services))
            handlers.add(airDropServerInitializer)
        }
        if (envManager.isBasServer) {
            handlers.add(ServerInitializerBas(services))
            handlers.add(airDropServerInitializer)
        }
        if (envManager.isBnbServer) {
            handlers.add(ServerInitializerBnbPol(services))
        }

        handlers.forEach {
            it.initHandlers(helper)
            it.initStreamListeners()
            it.initSchedulers()
        }

        helper.dispose()
    }

    private fun initEvents() {
        addEventHandler(SFSEventType.USER_DISCONNECT, UserLogoutHandler::class.java)
        addEventHandler(SFSEventType.USER_LOGOUT, UserLogoutHandler::class.java)
        addEventHandler(SFSEventType.USER_LOGIN, UserLoginHandler::class.java)
        addEventHandler(SFSEventType.USER_JOIN_ZONE, UserJoinZoneHandler::class.java)
    }
}