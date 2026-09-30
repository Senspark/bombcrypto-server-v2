package com.senspark.game.manager

import com.senspark.common.service.IGlobalService
import com.senspark.common.service.IService
import com.senspark.common.service.Service
import com.senspark.common.utils.AppStage
import com.senspark.common.utils.RemoteLoggerInitData

interface IPvpEnvManager : IGlobalService {
    val isGke: Boolean
    val appStage: AppStage
    val serverId: String
    val apiUrl: String
    
    val postgresDriverName: String
    val postgresConnectionString: String
    val postgresUsername: String
    val postgresPassword: String
    val postgresMaxActiveConnections: Int
    val postgresTestSql: String

    val redisConnectionString: String
    val redisConsumerId: String

    val logRemoteData: RemoteLoggerInitData
}