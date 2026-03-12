package com.senspark.game.data.model.user

enum class UserPrivilege(val value: Int) {
    User(0),
    Moderator(1),
    Admin(2)
    ;

    companion object {
        private val map = entries.associateBy(UserPrivilege::value)
        fun fromInt(typeValue: Int): UserPrivilege {
            if (map.containsKey(typeValue)) {
                return map[typeValue]!!
            }
            return User
        }
    }
}