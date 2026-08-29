package com.senspark.game.extension

import com.senspark.common.extension.IApiExtension
import com.senspark.game.pvp.handler.UserDisconnectHandler
import com.senspark.game.pvp.handler.UserJoinZoneHandler
import com.senspark.game.pvp.handler.UserLoginHandler
import com.senspark.game.pvp.handler.UserLogoutHandler
import com.smartfoxserver.v2.core.SFSEventType
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.extensions.SFSExtension

/** Used by zone extension. */
@Suppress("unused")
class PvpZoneExtension : SFSExtension(), IApiExtension {
    companion object {
        private lateinit var allServices: PvpServices
        val AllServices: PvpServices
            get() {
                return allServices
            }
    }
    
    override fun init() {
        allServices = PvpZoneExtensionModules.initModules(this)
        println("====== SERVER ID ${allServices.envManager.serverId} ========")
        registerHandlers()
        registerScheduledTasks()
        allServices.cacheService.test()
    }

    override fun destroy() {
        allServices.scheduler.clearAll()
    }

    private fun registerHandlers() {
        addEventHandler(SFSEventType.USER_LOGIN, UserLoginHandler::class.java)
        addEventHandler(SFSEventType.USER_JOIN_ZONE, UserJoinZoneHandler::class.java)
        addEventHandler(SFSEventType.USER_DISCONNECT, UserDisconnectHandler::class.java)
        addEventHandler(SFSEventType.USER_LOGOUT, UserLogoutHandler::class.java)
    }
    
    private fun registerScheduledTasks() {
        updateServerInfo()
        registerRankingResetTask()
        registerFeeProcessingTask()
    }
    
    private fun registerFeeProcessingTask() {
        allServices.scheduler.schedule("feeProcessingTask", 0, 3600 * 1000) { // Every hour
            val processor = com.senspark.game.pvp.service.PvpFeeProcessor(
                allServices.database,
                allServices.logger,
                allServices.pvpDataAccess.statement // Accessing statement from dataAccess
            )
            processor.processPendingFees()
        }
    }
    
    private fun registerRankingResetTask() {
        allServices.scheduler.schedule("rankingResetTask", 0, 3600 * 1000) { // Every hour
            val task = com.senspark.game.pvp.manager.PvpRankingResetTask(allServices.database, allServices.logger)
            task.run()
        }
    }
    
    private fun updateServerInfo(){
        allServices.scheduler.schedule("updateServerInfo", 0, allServices.serverInfoManager.getServerInfoTimeUpdate() * 1000) {
            allServices.serverInfoManager.updateUserOnlineToRedis()
        }
    }

    override fun handleInternalMessage(cmdName: String, obj: Any): Any? {
        return allServices.internalMessageHandler.handle(cmdName, obj as ISFSObject)
    }
}