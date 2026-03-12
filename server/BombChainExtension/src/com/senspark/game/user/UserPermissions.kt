package com.senspark.game.user

import com.senspark.game.data.UserPermissionsData

class UserPermissions(data: UserPermissionsData) : IUserPermissions {
    override val createRoom = data.createRoom
    override val resetData = data.resetData
    override val storyImmortal = data.storyImmortal
    override val storyOneHit = data.storyOneHit
    override val viewPvPQueueUser = data.viewPvPDashboard

    override fun destroy() = Unit
}