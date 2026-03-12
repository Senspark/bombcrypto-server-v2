package com.senspark.game.data.model.auth

import com.senspark.game.declare.EnumConstants.UserType

class UserLoginInfo(
    override val userId: Int,
    override val username: String,
    override val loginUsername: String?,
    override val displayName: String?,
    override val email: String?,
    override val userType: UserType,
    override val hasPasscode: Boolean,
    override val createAt: Long,
) : IUserLoginInfo