package com.senspark.game.user

import com.senspark.common.service.IService
import com.senspark.common.service.Service

@Service("IUserPermissions")
interface IUserPermissions : IService {
    val createRoom: Boolean
    val resetData: Boolean
    val storyImmortal: Boolean
    val storyOneHit: Boolean
    val viewPvPQueueUser: Boolean
}