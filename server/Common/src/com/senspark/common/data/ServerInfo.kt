package com.senspark.common.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ServerInfo(
    @SerialName("users") override val users: Int,
    @SerialName("max_users") override val maxUsers: Int,
    @SerialName("is_maintenance") override val isMaintenance: Boolean,
    @SerialName("maintenance_timestamp") override val maintenanceTimestamp: Long,
) : IServerInfo