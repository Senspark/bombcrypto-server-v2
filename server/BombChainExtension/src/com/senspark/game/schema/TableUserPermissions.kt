package com.senspark.game.schema

import org.jetbrains.exposed.sql.Table

object TableUserPermissions : Table("user_permissions") {
    val createRoom = bool("create_room")
    val resetData = bool("reset_data")
    val storyImmortal = bool("story_immortal")
    val storyOneHit = bool("story_one_hit")
    val viewPvPDashboard = bool("view_pvp_dashboard")
    val userId = integer("user_id")
}