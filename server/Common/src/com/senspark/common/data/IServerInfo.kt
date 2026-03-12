package com.senspark.common.data

interface IServerInfo {
    val users: Int
    val maxUsers: Int
    val isMaintenance: Boolean
    val maintenanceTimestamp: Long
}